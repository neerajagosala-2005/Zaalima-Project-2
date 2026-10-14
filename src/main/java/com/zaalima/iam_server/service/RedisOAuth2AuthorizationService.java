package com.zaalima.iam_server.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module;
import org.springframework.stereotype.Service;

@Service
public class RedisOAuth2AuthorizationService
        implements OAuth2AuthorizationService {

    private static final String AUTHORIZATION_PREFIX =
            "oauth2:authorization:";

    private static final String TOKEN_PREFIX =
            "oauth2:token:";

    private final StringRedisTemplate redisTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper;

    public RedisOAuth2AuthorizationService(
            StringRedisTemplate redisTemplate,
            RegisteredClientRepository registeredClientRepository) {

        this.redisTemplate = redisTemplate;
        this.registeredClientRepository = registeredClientRepository;

        this.objectMapper = new ObjectMapper();

        this.objectMapper.registerModules(
                SecurityJackson2Modules.getModules(
                        getClass().getClassLoader()));

        this.objectMapper.registerModule(
                new OAuth2AuthorizationServerJackson2Module());
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        try {
            String json =
                    objectMapper.writeValueAsString(authorization);

            redisTemplate.opsForValue().set(
                    AUTHORIZATION_PREFIX + authorization.getId(),
                    json);

            indexToken(
                    authorization.getAccessToken() != null
                            ? authorization.getAccessToken()
                                    .getToken().getTokenValue()
                            : null,
                    authorization.getId());

            indexToken(
                    authorization.getRefreshToken() != null
                            ? authorization.getRefreshToken()
                                    .getToken().getTokenValue()
                            : null,
                    authorization.getId());

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to save OAuth2 authorization in Redis",
                    ex);
        }
    }

    private void indexToken(
            String token,
            String authorizationId) {

        if (token != null) {
            redisTemplate.opsForValue().set(
                    TOKEN_PREFIX + token,
                    authorizationId);
        }
    }

    @Override
    public void remove(OAuth2Authorization authorization) {

        redisTemplate.delete(
                AUTHORIZATION_PREFIX + authorization.getId());

        if (authorization.getAccessToken() != null) {
            redisTemplate.delete(
                    TOKEN_PREFIX
                            + authorization.getAccessToken()
                                    .getToken().getTokenValue());
        }

        if (authorization.getRefreshToken() != null) {
            redisTemplate.delete(
                    TOKEN_PREFIX
                            + authorization.getRefreshToken()
                                    .getToken().getTokenValue());
        }
    }

    @Override
    public OAuth2Authorization findById(String id) {

        String json = redisTemplate.opsForValue().get(
                AUTHORIZATION_PREFIX + id);

        return deserialize(json);
    }

    @Override
    public OAuth2Authorization findByToken(
            String token,
            OAuth2TokenType tokenType) {

        if (token == null) {
            return null;
        }

        String authorizationId =
                redisTemplate.opsForValue().get(
                        TOKEN_PREFIX + token);

        if (authorizationId == null) {
            return null;
        }

        OAuth2Authorization authorization =
                findById(authorizationId);

        if (authorization == null) {
            return null;
        }

        OAuth2Authorization.Token<?> foundToken =
                authorization.getToken(token);

        if (foundToken == null) {
            return null;
        }

        if (tokenType != null) {

            String expectedTokenType = tokenType.getValue();
            String actualTokenType = null;

            if (authorization.getAccessToken() != null
                    && authorization.getAccessToken()
                            .getToken().getTokenValue()
                            .equals(token)) {

                actualTokenType = "access_token";

            } else if (authorization.getRefreshToken() != null
                    && authorization.getRefreshToken()
                            .getToken().getTokenValue()
                            .equals(token)) {

                actualTokenType = "refresh_token";
            }

            if (actualTokenType == null
                    || !actualTokenType.equals(expectedTokenType)) {
                return null;
            }
        }

        return authorization;
    }

    private OAuth2Authorization deserialize(String json) {

        if (json == null) {
            return null;
        }

        try {
            return objectMapper.readValue(
                    json,
                    OAuth2Authorization.class);

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to read OAuth2 authorization from Redis",
                    ex);
        }
    }
}