package com.zaalima.iam_server.controller;

import com.zaalima.iam_server.entity.User;
import com.zaalima.iam_server.repository.UserRepository;
import com.zaalima.iam_server.service.AuditLogService;
import com.zaalima.iam_server.service.TotpService;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/mfa")
public class MfaController {

    private final UserRepository userRepository;
    private final TotpService totpService;
    private final AuditLogService auditLogService;

    public MfaController(
            UserRepository userRepository,
            TotpService totpService,
            AuditLogService auditLogService) {

        this.userRepository = userRepository;
        this.totpService = totpService;
        this.auditLogService = auditLogService;
    }

    // ==============================
    // MFA SETUP
    // ==============================

    @PostMapping("/setup")
    public ResponseEntity<?> setupMfa(
            org.springframework.security.core.Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    username,
                    "MFA_SETUP",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity
                    .status(404)
                    .body("User not found");
        }

        // Generate a fresh secret
        String secret = totpService.generateSecret();

        user.setMfaSecret(secret);
        user.setMfaEnabled(false);

        userRepository.save(user);

        auditLogService.log(
                username,
                "MFA_SETUP",
                "SUCCESS",
                httpRequest
        );

        String encodedUsername =
                URLEncoder.encode(
                        username,
                        StandardCharsets.UTF_8
                );

        /*
         * TOTP configuration:
         * Algorithm = SHA1
         * Digits    = 6
         * Period    = 30 seconds
         */
        String otpAuthUrl =
                "otpauth://totp/Zaalima%20IAM:"
                + encodedUsername
                + "?secret="
                + secret
                + "&issuer=Zaalima%20IAM"
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";

        return ResponseEntity.ok(
                new MfaSetupResponse(
                        username,
                        secret,
                        otpAuthUrl
                )
        );
    }

    // ==============================
    // MFA VERIFY
    // ==============================

    @PostMapping("/verify")
    public ResponseEntity<String> verifyMfa(
            @RequestParam String code,
            org.springframework.security.core.Authentication authentication,
            HttpServletRequest httpRequest) {

        String username = authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    username,
                    "MFA_VERIFY",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity
                    .status(404)
                    .body("User not found");
        }

        if (user.getMfaSecret() == null) {

            auditLogService.log(
                    username,
                    "MFA_VERIFY",
                    "FAILED_SETUP_NOT_STARTED",
                    httpRequest
            );

            return ResponseEntity
                    .badRequest()
                    .body("MFA setup not started");
        }

        // Debug: confirm that this method is reached
        System.out.println(
                "MFA VERIFY METHOD CALLED"
        );

        boolean valid =
                totpService.verifyCode(
                        user.getMfaSecret(),
                        code
                );

        if (!valid) {

            auditLogService.log(
                    username,
                    "MFA_VERIFY",
                    "FAILED_INVALID_CODE",
                    httpRequest
            );

            return ResponseEntity
                    .status(401)
                    .body("Invalid MFA code");
        }

        // MFA verification successful
        user.setMfaEnabled(true);

        userRepository.save(user);

        auditLogService.log(
                username,
                "MFA_VERIFY",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                "MFA enabled successfully"
        );
    }

    // ==============================
    // MFA SETUP RESPONSE
    // ==============================

    public static class MfaSetupResponse {

        private String username;
        private String secret;
        private String otpAuthUrl;

        public MfaSetupResponse(
                String username,
                String secret,
                String otpAuthUrl) {

            this.username = username;
            this.secret = secret;
            this.otpAuthUrl = otpAuthUrl;
        }

        public String getUsername() {
            return username;
        }

        public String getSecret() {
            return secret;
        }

        public String getOtpAuthUrl() {
            return otpAuthUrl;
        }
    }
}