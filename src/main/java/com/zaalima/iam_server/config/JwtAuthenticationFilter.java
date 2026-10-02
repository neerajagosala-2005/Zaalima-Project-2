package com.zaalima.iam_server.config;

import com.zaalima.iam_server.entity.Role;
import com.zaalima.iam_server.entity.User;
import com.zaalima.iam_server.repository.UserRepository;
import com.zaalima.iam_server.service.RedisService;
import com.zaalima.iam_server.util.JwtUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisService redisService;

    public JwtAuthenticationFilter(
            JwtUtil jwtUtil,
            UserRepository userRepository,
            RedisService redisService) {

        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader =
                request.getHeader("Authorization");

        if (authHeader != null &&
                authHeader.startsWith("Bearer ")) {

            String token =
                    authHeader.substring(7);

            if (jwtUtil.validateToken(token)) {

                String username =
                        jwtUtil.extractUsername(token);

                /*
                 * Check whether this exact token
                 * is still active in Redis.
                 */
                String storedToken =
                        redisService.getLoginSession(username);

                boolean redisSessionValid =
                        token.equals(storedToken);

                if (redisSessionValid) {

                    User user =
                            userRepository
                                    .findByUsername(username)
                                    .orElse(null);

                    if (user != null &&
                            user.isEnabled()) {

                        List<SimpleGrantedAuthority> authorities =
                                new ArrayList<>();

                        for (Role role : user.getRoles()) {

                            authorities.add(
                                    new SimpleGrantedAuthority(
                                            "ROLE_" + role.getName()
                                    )
                            );

                            role.getAuthorities()
                                    .forEach(authority ->
                                            authorities.add(
                                                    new SimpleGrantedAuthority(
                                                            authority.getName()
                                                    )
                                            )
                                    );
                        }

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        username,
                                        null,
                                        authorities
                                );

                        SecurityContextHolder
                                .getContext()
                                .setAuthentication(authentication);
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}