package com.kaushal.mfa_app.controller;

import com.kaushal.mfa_app.dto.*;
import com.kaushal.mfa_app.service.UserService;
import com.kaushal.mfa_app.util.RedisServiceUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisServiceUtil redisServiceUtil;

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerUser(@RequestBody RegisterRequest registerRequest) {
        try {
            Map<String, String> response = userService.registerUser(registerRequest);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("Error", "Registration failed: "+e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/delete-user")
    public ResponseEntity<String> deleteUser(@RequestBody UserDeleteRequest request) {

        Map<String, String> response = userService.deleteUserByEmail(request.getEmail());
        Set keys = response.keySet();
        if (keys.contains("ERROR")) {
            return ResponseEntity.status(404).body(response.get("ERROR"));
        }
        return new ResponseEntity<>(response.get("Message"), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginRequest loginRequest) {
        String result = userService.loginUser(loginRequest);
        if (result.contains("Invalid email or password.")) {
            return ResponseEntity.status(400).body("Invalid email or password.");
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/email-login")
    public ResponseEntity<String> loginUser(@RequestBody EmailLoginRequest emailLoginRequest) {
        String result = userService.loginWithEmail(emailLoginRequest.getEmail());
        if (result.contains("Invalid email or password.")) {
            return ResponseEntity.status(400).body("Invalid email or password.");
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping("/cache-email")
    public ResponseEntity<String> cacheEmailAndReturnToken(@RequestBody EmailLoginRequest request, HttpServletResponse response) {
        String token = userService.cacheEmailAndGenerateToken(request.getEmail());

        if ("User not found!".equals(token)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(token);
        }

        System.out.println("Generated token: [" + token + "]");

        ResponseCookie cookie = ResponseCookie.from("login_token", token)
                .httpOnly(false)
                .secure(false)
                .sameSite("Lax") // Or "None" if you're testing across different ports/origins
                .path("/")
                .maxAge(Duration.ofDays(30))
                .build();

        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok("Token stored in cookie.");
    }

    @GetMapping("/check-login-token")
    public ResponseEntity<Map<String, String>> checkLoginTokenFromCookie(@CookieValue(name = "login_token", required = false) String token) {

        Map<String, String> response = new HashMap<>();
        response.put("Message", null);
        if (token == null) {
            response.put("Message", "Token not found. Please login with email.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        boolean isTokenValid = redisServiceUtil.isTokenValid(token);
        if (!isTokenValid) {
            response.put("Message", "Token is not valid. Please login with email.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String email = redisServiceUtil.getEmailByToken(token);
        response.put("Message", email);
        return ResponseEntity.ok(response);
    }

    @CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
    @GetMapping("/get-email-by-token")
    public ResponseEntity<String> getEmailByToken(@CookieValue(name = "login_token", required = false) String token, HttpServletResponse response) {
        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token not stored in cookies.");
        }

        response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
        response.setHeader("Access-Control-Allow-Credentials", "true");

        boolean isTokenValid = redisServiceUtil.isTokenValid(token);
        if (!isTokenValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token.");
        }
        String email = userService.getEmailByToken(token);
        return ResponseEntity.ok(email);
    }

    @PostMapping("/is-code-valid")
    public ResponseEntity<AuthTokenTransfer> isAuthCodeValid(@RequestBody CodeCheckRequest codeCheckRequest) {
        try {
            return new ResponseEntity<>(userService.isAuthCodeValid(codeCheckRequest.getEmail(),
                    codeCheckRequest.getSixDigitCode()), HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(400).body(new AuthTokenTransfer("Failed to generate auth token", "ERROR IN TOKEN GENERATION."));
        }
    }

}
