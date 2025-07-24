package com.kaushal.mfa_app.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceUtil {

    private static final long TTL_DAYS = 30;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public void storeEmailWithToken(String token, String email) {
        String emailKey = "email:" + email;

        // Delete old token if exists
        String oldToken = redisTemplate.opsForValue().get(emailKey);
        if (oldToken != null) {
            String oldLoginKey = "login:" + oldToken;
            redisTemplate.delete(oldLoginKey);
        }

        // Store new token → email
        String newLoginKey = "login:" + token;
        redisTemplate.opsForValue().set(newLoginKey, email, TTL_DAYS, TimeUnit.DAYS);

        // Store email → token
        redisTemplate.opsForValue().set(emailKey, token, TTL_DAYS, TimeUnit.DAYS);
    }

    public String getEmailByToken(String token) {
        String key = "login:" + token;
        return redisTemplate.opsForValue().get(key);
    }

    public String getTokenByEmail(String email) {
        String emailKey = "email:" + email;
        return redisTemplate.opsForValue().get(emailKey);
    }

    public void deleteEmailByToken(String token) {
        String key = "login:" + token;
        redisTemplate.delete(key);
    }

    public boolean isTokenValid(String token) {
        String key = "login:" + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

}
