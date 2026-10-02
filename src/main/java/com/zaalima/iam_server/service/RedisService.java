package com.zaalima.iam_server.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    public RedisService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void saveLoginSession(
            String username,
            String token,
            long expirationSeconds) {

        redisTemplate.opsForValue().set(
                "login:session:" + username,
                token,
                Duration.ofSeconds(expirationSeconds)
        );
    }

    public String getLoginSession(String username) {

        return redisTemplate.opsForValue()
                .get("login:session:" + username);
    }

    public void deleteLoginSession(String username) {

        redisTemplate.delete(
                "login:session:" + username
        );
    }

    public boolean sessionExists(String username) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(
                        "login:session:" + username
                )
        );
    }
}