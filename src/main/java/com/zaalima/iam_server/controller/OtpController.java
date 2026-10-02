package com.zaalima.iam_server.controller;

import com.zaalima.iam_server.entity.User;
import com.zaalima.iam_server.repository.UserRepository;
import com.zaalima.iam_server.service.AuditLogService;
import com.zaalima.iam_server.service.OtpService;
import com.zaalima.iam_server.service.RateLimitService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/otp")
public class OtpController {

    private final OtpService otpService;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;

    public OtpController(
            OtpService otpService,
            UserRepository userRepository,
            AuditLogService auditLogService,
            RateLimitService rateLimitService) {

        this.otpService = otpService;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateOtp(
            @RequestParam String username,
            HttpServletRequest httpRequest) {

        // Maximum 3 OTP generation requests per minute
        String rateLimitKey =
                "rate:otp-generate:" + username;

        boolean allowed =
                rateLimitService.isAllowed(
                        rateLimitKey,
                        3,
                        Duration.ofMinutes(1)
                );

        if (!allowed) {

            auditLogService.log(
                    username,
                    "OTP_GENERATE",
                    "RATE_LIMITED",
                    httpRequest
            );

            return ResponseEntity.status(429)
                    .body("Too many OTP requests. Please try again later.");
        }

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    username,
                    "OTP_GENERATE",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity.status(404)
                    .body("User not found");
        }

        otpService.generateOtp(
                user.getUsername(),
                user.getEmail()
        );

        auditLogService.log(
                username,
                "OTP_GENERATE",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                "OTP sent successfully to registered email"
        );
    }

    @PostMapping("/verify")
    public ResponseEntity<String> verifyOtp(
            @RequestParam String username,
            @RequestParam String otp,
            HttpServletRequest httpRequest) {

        // Maximum 5 OTP verification attempts per minute
        String rateLimitKey =
                "rate:otp-verify:" + username;

        boolean allowed =
                rateLimitService.isAllowed(
                        rateLimitKey,
                        5,
                        Duration.ofMinutes(1)
                );

        if (!allowed) {

            auditLogService.log(
                    username,
                    "OTP_VERIFY",
                    "RATE_LIMITED",
                    httpRequest
            );

            return ResponseEntity.status(429)
                    .body("Too many OTP verification attempts. Please try again later.");
        }

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    username,
                    "OTP_VERIFY",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity.status(404)
                    .body("User not found");
        }

        boolean valid = otpService.verifyOtp(
                username,
                otp
        );

        if (!valid) {

            auditLogService.log(
                    username,
                    "OTP_VERIFY",
                    "FAILED_INVALID_OR_EXPIRED",
                    httpRequest
            );

            return ResponseEntity.status(401)
                    .body("Invalid or expired OTP");
        }

        auditLogService.log(
                username,
                "OTP_VERIFY",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                "OTP verified successfully"
        );
    }
}