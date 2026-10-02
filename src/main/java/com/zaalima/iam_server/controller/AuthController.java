package com.zaalima.iam_server.controller;

import com.zaalima.iam_server.dto.LoginRequest;
import com.zaalima.iam_server.dto.LoginResponse;
import com.zaalima.iam_server.dto.PasswordResetRequest;
import com.zaalima.iam_server.dto.RegisterRequest;
import com.zaalima.iam_server.dto.UserResponse;
import com.zaalima.iam_server.entity.Authority;
import com.zaalima.iam_server.entity.Role;
import com.zaalima.iam_server.entity.User;
import com.zaalima.iam_server.repository.AuthorityRepository;
import com.zaalima.iam_server.repository.RoleRepository;
import com.zaalima.iam_server.repository.UserRepository;
import com.zaalima.iam_server.service.AuditLogService;
import com.zaalima.iam_server.service.RateLimitService;
import com.zaalima.iam_server.service.RedisService;
import com.zaalima.iam_server.service.TotpService;
import com.zaalima.iam_server.service.OtpService;
import com.zaalima.iam_server.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.List;
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final TotpService totpService;
    private final OtpService otpService;
    private final RedisService redisService;
    private final AuditLogService auditLogService;
    private final RateLimitService rateLimitService;

    public AuthController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthorityRepository authorityRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            TotpService totpService,
            OtpService otpService,
            RedisService redisService,
            AuditLogService auditLogService,
            RateLimitService rateLimitService) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.totpService = totpService;
        this.otpService = otpService;
        this.redisService = redisService;
        this.auditLogService = auditLogService;
        this.rateLimitService = rateLimitService;
    }

    // =========================
    // REGISTER
    // =========================

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        if (userRepository.findByUsername(
                request.getUsername()).isPresent()) {

            auditLogService.log(
                    request.getUsername(),
                    "REGISTER",
                    "FAILED_USERNAME_EXISTS",
                    httpRequest
            );

            return ResponseEntity.badRequest()
                    .body("Username already exists");
        }

        if (userRepository.findByEmail(
                request.getEmail()).isPresent()) {

            auditLogService.log(
                    request.getUsername(),
                    "REGISTER",
                    "FAILED_EMAIL_EXISTS",
                    httpRequest
            );

            return ResponseEntity.badRequest()
                    .body("Email already exists");
        }

        Authority readAuthority = authorityRepository
                .findByName("READ")
                .orElseGet(() -> {

                    Authority authority = new Authority();
                    authority.setName("READ");

                    return authorityRepository.save(authority);
                });

        Role userRole = roleRepository
                .findByName("USER")
                .orElseGet(() -> {

                    Role newRole = new Role();
                    newRole.setName("USER");

                    return roleRepository.save(newRole);
                });

        userRole.getAuthorities().add(readAuthority);

        userRole = roleRepository.save(userRole);

        User user = new User();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());

        user.setPassword(
                passwordEncoder.encode(
                        request.getPassword()
                )
        );

        user.setEnabled(true);
        user.setMfaEnabled(false);

        user.getRoles().add(userRole);

        userRepository.save(user);

        auditLogService.log(
                user.getUsername(),
                "REGISTER",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                "User registered successfully"
        );
    }

    // =========================
    // LOGIN
    // =========================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        String rateLimitKey =
                "rate:login:" + request.getUsername();

        boolean allowed =
                rateLimitService.isAllowed(
                        rateLimitKey,
                        5,
                        Duration.ofMinutes(1)
                );

        if (!allowed) {

            auditLogService.log(
                    request.getUsername(),
                    "LOGIN",
                    "RATE_LIMITED",
                    httpRequest
            );

            return ResponseEntity.status(429)
                    .body(
                            "Too many login attempts. " +
                            "Please try again later."
                    );
        }

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    request.getUsername(),
                    "LOGIN",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity.status(401)
                    .body("Invalid username or password");
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            auditLogService.log(
                    user.getUsername(),
                    "LOGIN",
                    "FAILED_WRONG_PASSWORD",
                    httpRequest
            );

            return ResponseEntity.status(401)
                    .body("Invalid username or password");
        }

        if (!user.isEnabled()) {

            auditLogService.log(
                    user.getUsername(),
                    "LOGIN",
                    "FAILED_ACCOUNT_DISABLED",
                    httpRequest
            );

            return ResponseEntity.status(403)
                    .body("User account is disabled");
        }

        if (user.isMfaEnabled()) {

            auditLogService.log(
                    user.getUsername(),
                    "LOGIN",
                    "MFA_REQUIRED",
                    httpRequest
            );

            return ResponseEntity.ok(
                    new LoginResponse(
                            "MFA verification required",
                            null,
                            true
                    )
            );
        }

        String token =
                generateTokenForUser(user);

        redisService.saveLoginSession(
                user.getUsername(),
                token,
                3600
        );

        auditLogService.log(
                user.getUsername(),
                "LOGIN",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        "Login successful",
                        token,
                        false
                )
        );
    }

    // =========================
    // MFA LOGIN
    // =========================

    @PostMapping("/login/mfa")
    public ResponseEntity<?> verifyLoginMfa(
            @RequestParam String username,
            @RequestParam String code,
            HttpServletRequest httpRequest) {

        String rateLimitKey =
                "rate:mfa-login:" + username;

        boolean allowed =
                rateLimitService.isAllowed(
                        rateLimitKey,
                        5,
                        Duration.ofMinutes(1)
                );

        if (!allowed) {

            auditLogService.log(
                    username,
                    "MFA_LOGIN",
                    "RATE_LIMITED",
                    httpRequest
            );

            return ResponseEntity.status(429)
                    .body(
                            "Too many MFA attempts. " +
                            "Please try again later."
                    );
        }

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    username,
                    "MFA_LOGIN",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity.status(401)
                    .body("Invalid username");
        }

        if (!user.isEnabled()) {

            auditLogService.log(
                    username,
                    "MFA_LOGIN",
                    "FAILED_ACCOUNT_DISABLED",
                    httpRequest
            );

            return ResponseEntity.status(403)
                    .body("User account is disabled");
        }

        if (!user.isMfaEnabled()) {

            return ResponseEntity.badRequest()
                    .body(
                            "MFA is not enabled for this user"
                    );
        }

        if (user.getMfaSecret() == null) {

            return ResponseEntity.badRequest()
                    .body(
                            "MFA secret is not configured"
                    );
        }

        boolean valid =
                totpService.verifyCode(
                        user.getMfaSecret(),
                        code
                );

        if (!valid) {

            auditLogService.log(
                    username,
                    "MFA_LOGIN",
                    "FAILED_INVALID_MFA_CODE",
                    httpRequest
            );

            return ResponseEntity.status(401)
                    .body("Invalid MFA code");
        }

        String token =
                generateTokenForUser(user);

        redisService.saveLoginSession(
                user.getUsername(),
                token,
                3600
        );

        auditLogService.log(
                username,
                "MFA_LOGIN",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                new LoginResponse(
                        "MFA verification successful",
                        token,
                        false
                )
        );
    }

    // =========================
    // PROFILE
    // =========================

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(
            org.springframework.security.core.Authentication authentication) {

        String username =
                authentication.getName();

        User user = userRepository
                .findByUsername(username)
                .orElse(null);

        if (user == null) {

            return ResponseEntity.status(404)
                    .body("User not found");
        }

        UserResponse response =
                new UserResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.isMfaEnabled()
                );

        return ResponseEntity.ok(response);
    }

    // =========================
    // RESET PASSWORD
    // =========================

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        User user = userRepository
                .findByUsername(request.getUsername())
                .orElse(null);

        if (user == null) {

            auditLogService.log(
                    request.getUsername(),
                    "PASSWORD_RESET",
                    "FAILED_USER_NOT_FOUND",
                    httpRequest
            );

            return ResponseEntity
                    .status(404)
                    .body("User not found");
        }

        // Verify OTP before allowing password reset
        boolean otpValid = otpService.verifyOtp(
                request.getUsername(),
                request.getOtp()
        );

        if (!otpValid) {

            auditLogService.log(
                    user.getUsername(),
                    "PASSWORD_RESET",
                    "FAILED_INVALID_OR_EXPIRED_OTP",
                    httpRequest
            );

            return ResponseEntity
                    .status(401)
                    .body("Invalid or expired OTP");
        }

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()
                )
        );

        userRepository.save(user);

        auditLogService.log(
                user.getUsername(),
                "PASSWORD_RESET",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                "Password reset successfully"
        );
    }

    // =========================
    // GET USERS
    // =========================

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {

        var users = userRepository
                .findAll()
                .stream()
                .map(user ->
                        new UserResponse(
                                user.getId(),
                                user.getUsername(),
                                user.getEmail(),
                                user.isEnabled(),
                                user.isMfaEnabled()
                        )
                )
                .toList();

        return ResponseEntity.ok(users);
    }

    // =========================
    // READ TEST
    // =========================

    @GetMapping("/read-test")
    public ResponseEntity<String> readTest() {

        return ResponseEntity.ok(
                "READ authority verified successfully"
        );
    }

    // =========================
    // WRITE TEST
    // =========================

    @GetMapping("/write-test")
    public ResponseEntity<String> writeTest() {

        return ResponseEntity.ok(
                "WRITE authority verified successfully"
        );
    }

    // =========================
    // LOGOUT / TOKEN REVOCATION
    // =========================

    @PostMapping("/logout")
    public ResponseEntity<String> logout(
            org.springframework.security.core.Authentication authentication,
            HttpServletRequest httpRequest) {

        String username =
                authentication.getName();

        // Delete active JWT session from Redis
        redisService.deleteLoginSession(username);

        // Audit logout
        auditLogService.log(
                username,
                "LOGOUT",
                "SUCCESS",
                httpRequest
        );

        return ResponseEntity.ok(
                "Logout successful. Session revoked."
        );
    }

    // =========================
    // JWT TOKEN GENERATION
    // =========================

    private String generateTokenForUser(User user) {

        List<String> roles = user.getRoles()
                .stream()
                .map(Role::getName)
                .toList();

        List<String> permissions = user.getRoles()
                .stream()
                .flatMap(role ->
                        role.getAuthorities()
                                .stream()
                                .map(Authority::getName)
                )
                .distinct()
                .toList();

        return jwtUtil.generateToken(
                user.getUsername(),
                roles,
                permissions
        );
    }
}