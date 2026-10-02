package com.zaalima.iam_server.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private final SecureRandom secureRandom =
            new SecureRandom();

    public OtpService(
            StringRedisTemplate redisTemplate,
            EmailService emailService) {

        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
    }

    public String generateOtp(
            String username,
            String email) {

        String otp =
                String.format(
                        "%06d",
                        secureRandom.nextInt(1_000_000)
                );

        // Store OTP in Redis for 5 minutes
        redisTemplate.opsForValue().set(
                "otp:" + username,
                otp,
                Duration.ofMinutes(5)
        );

        // Send OTP through SendGrid
        emailService.sendOtpEmail(
                email,
                otp
        );

        return otp;
    }

    public boolean verifyOtp(
            String username,
            String otp) {

        String storedOtp =
                redisTemplate.opsForValue()
                        .get("otp:" + username);

        if (storedOtp == null) {
            return false;
        }

        boolean valid =
                storedOtp.equals(otp);

        if (valid) {

            // OTP can be used only once
            redisTemplate.delete(
                    "otp:" + username
            );
        }

        return valid;
    }
}