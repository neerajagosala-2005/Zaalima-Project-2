package com.zaalima.iam_server.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

@Configuration
public class AuthorizationServerConfig {

    private final PasswordEncoder passwordEncoder;

    public AuthorizationServerConfig(
            PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerSecurityFilterChain(
            HttpSecurity http,
            OAuth2AuthorizationService authorizationService)
            throws Exception {

        OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
                new OAuth2AuthorizationServerConfigurer();

        http
            .securityMatcher(
                authorizationServerConfigurer.getEndpointsMatcher()
            )
            .with(
                authorizationServerConfigurer,
                authorizationServer -> authorizationServer
                    .authorizationService(authorizationService)
                    .oidc(Customizer.withDefaults())
                    .authorizationEndpoint(
                        authorizationEndpoint ->
                            authorizationEndpoint
                                .consentPage("/oauth2/consent")
                    )
            )
            .authorizeHttpRequests(
                authorize -> authorize.anyRequest().authenticated()
            )
            .exceptionHandling(
                exceptionHandling ->
                    exceptionHandling.authenticationEntryPoint(
                        new LoginUrlAuthenticationEntryPoint("/login")
                    )
            );

        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository() {

        RegisteredClient client =
                RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId("zaalima-client")
                    .clientSecret(
                        passwordEncoder.encode("zaalima-secret")
                    )
                    .clientAuthenticationMethod(
                        ClientAuthenticationMethod.CLIENT_SECRET_POST
                    )
                    .authorizationGrantType(
                        AuthorizationGrantType.CLIENT_CREDENTIALS
                    )
                    .authorizationGrantType(
                        AuthorizationGrantType.AUTHORIZATION_CODE
                    )
                    .authorizationGrantType(
                        AuthorizationGrantType.REFRESH_TOKEN
                    )
                    .redirectUri(
                        "http://127.0.0.1:8081/login/oauth2/code/zaalima-client"
                    )
                    .scope("openid")
                    .scope("profile")
                    .scope("read")
                    .scope("write")
                    .clientSettings(
                        ClientSettings.builder()
                            .requireAuthorizationConsent(true)
                            .build()
                    )
                    .build();

        return new InMemoryRegisteredClientRepository(client);
    }

    @Bean
    public KeyPair rsaKeyPair() {
        return generateRsaKey();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        RSAPrivateKey privateKey =
                (RSAPrivateKey) keyPair.getPrivate();

        RSAKey rsaKey =
                new RSAKey.Builder(publicKey)
                    .privateKey(privateKey)
                    .keyID(UUID.randomUUID().toString())
                    .build();

        JWKSet jwkSet = new JWKSet(rsaKey);

        return (jwkSelector, securityContext) ->
                jwkSelector.select(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(KeyPair keyPair) {

        RSAPublicKey publicKey =
                (RSAPublicKey) keyPair.getPublic();

        return NimbusJwtDecoder
                .withPublicKey(publicKey)
                .build();
    }

    private static KeyPair generateRsaKey() {

        try {
            KeyPairGenerator keyPairGenerator =
                    KeyPairGenerator.getInstance("RSA");

            keyPairGenerator.initialize(2048);

            return keyPairGenerator.generateKeyPair();

        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Failed to generate RSA key", ex);
        }
    }
}