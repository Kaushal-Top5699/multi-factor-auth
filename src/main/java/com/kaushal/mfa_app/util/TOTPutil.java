package com.kaushal.mfa_app.util;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import org.apache.commons.codec.binary.Base32;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;

public class TOTPutil {

    private static final TimeBasedOneTimePasswordGenerator totpGenerator = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(30));

    // 1. Generate a new secret key (random)
    public static SecretKey generateSecretKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(totpGenerator.getAlgorithm());
        keyGenerator.init(160);
        return keyGenerator.generateKey();
    }

    // 2. Return Base32-encoded version of that key
    public static String getBase32Secret(SecretKey secretKey) {
        Base32 base32 = new Base32();
        return base32.encodeToString(secretKey.getEncoded());
    }

    // 3. Generate a TOTP code from the base32 secret (for validation/debugging)
    public static String generateCurrentCode(String base32Secret) throws Exception {
        Base32 base32 = new Base32();
        byte[] secretBytes = base32.decode(base32Secret);
        SecretKey key = new SecretKeySpec(secretBytes, totpGenerator.getAlgorithm());
        return String.format("%06d", totpGenerator.generateOneTimePassword(key, Instant.now()));
    }

    // 4. Validate user's submitted code
    public static boolean isCodeValid(String base32Secret, String userCode) throws Exception {
        String currentCode = generateCurrentCode(base32Secret);
        return currentCode.equals(userCode);
    }

}
