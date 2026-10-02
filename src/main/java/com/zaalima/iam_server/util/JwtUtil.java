package com.zaalima.iam_server.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private static final String SECRET =
            "ZaalimaIamServerSecretKeyForJwtAuthentication2026";

    private final SecretKey secretKey =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    private final long EXPIRATION_TIME =
            1000 * 60 * 60; // 1 hour

    public String generateToken(
            String username,
            List<String> roles,
            List<String> permissions) {

        Date now = new Date();

        Date expiryDate =
                new Date(now.getTime() + EXPIRATION_TIME);

        return Jwts.builder()
                .subject(username)

                // Organizational roles
                .claim("roles", roles)

                // Permissions / authorities
                .claim("permissions", permissions)

                .issuedAt(now)
                .expiration(expiryDate)

                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {

        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean validateToken(String token) {

        try {

            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;

        } catch (Exception e) {

            return false;
        }
    }
}