package com.kaushal.mfa_app.service;

import com.kaushal.mfa_app.dto.AuthTokenTransfer;
import com.kaushal.mfa_app.dto.LoginRequest;
import com.kaushal.mfa_app.dto.RegisterRequest;
import com.kaushal.mfa_app.entity.User;
import com.kaushal.mfa_app.repository.UserRepository;
import com.kaushal.mfa_app.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordUtil passwordUtil;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private RedisServiceUtil redisServiceUtil;


    public String loginUser(LoginRequest loginRequest) {
        Optional<User> userOptional = userRepository.findByEmail(loginRequest.getEmail());
        if (userOptional.isEmpty()) {
            return "Invalid email or password.";
        }

        User currentUser = userOptional.get();
        boolean isPassValid = passwordUtil.verifyPassword(loginRequest.getPassword(), currentUser.getPassword());

        if (!isPassValid) {
            return "Invalid email or password.";
        }

        return "User credentials correct.";
    }

    public String loginWithEmail(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) {
            return "Invalid email.";
        }
        return "User credentials verified.";
    }

    public String cacheEmailAndGenerateToken(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isEmpty()) {
            return "User not found!";
        }
        String token = UUID.randomUUID().toString();
        redisServiceUtil.storeEmailWithToken(token, email);
        return token;
    }

    public String getEmailByToken(String token) {
        return redisServiceUtil.getEmailByToken(token);
    }

    public AuthTokenTransfer isAuthCodeValid(String email, String sixDigitCode) throws Exception {
        String secret = userRepository.findByEmail(email).get().getTotpSecret();

        boolean isValid = TOTPutil.isCodeValid(secret, sixDigitCode);
        if (!isValid) {
            return new AuthTokenTransfer("Failed to generate auth token", "ERROR IN TOKEN GENERATION.");
        }

        String authToken = jwtTokenUtil.generateToken(email);

        return new AuthTokenTransfer("User Authenticated.", authToken);

    }


    public Map<String, String> registerUser(RegisterRequest registerRequest) throws Exception {

        Map<String, String> response = new HashMap<>();

        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            response.put("Message", "User with email already exists!");
            return response;
        }

        SecretKey key = TOTPutil.generateSecretKey();
        String base32Secret = TOTPutil.getBase32Secret(key);

        String hashedPassword = passwordUtil.hashPassword(registerRequest.getPassword());

        User newUser = new User();
        newUser.setEmail(registerRequest.getEmail());
        newUser.setPassword(hashedPassword);
        newUser.setFirstName(registerRequest.getFirstName());
        newUser.setLastName(registerRequest.getLastName());
        newUser.setPhoneNum(registerRequest.getPhoneNum());
        newUser.setTotpSecret(base32Secret);

        userRepository.save(newUser);

        // 🔥 New line to send URI for QR code generation
        String otpAuthUri = String.format(
                "otpauth://totp/%s:%s?secret=%s&issuer=%s",
                "MFA-App", registerRequest.getEmail(), base32Secret, "MFA-App"
        );

        // ✅ Generate QR code for this URI
        String base64Qr = QRCodeUtil.generateQRCodeBase64(otpAuthUri, 250, 250);

        response.put("QRCodeLink", base64Qr);

        return response;
    }

    public Map<String, String> deleteUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        Map<String, String> response = new HashMap<>();
        if (user.isEmpty()) {
            response.put("ERROR", "User not found.");
            return response;
        }
        User currentUser = user.get();
        userRepository.delete(currentUser);
        response.put("Message", "User deleted.");
        return response;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return (UserDetails) userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}
