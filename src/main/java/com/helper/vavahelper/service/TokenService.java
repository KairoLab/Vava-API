package com.helper.vavahelper.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.helper.vavahelper.models.User.User;

import jakarta.annotation.PostConstruct;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);
    private static final String ISSUER = "auth-api";
    private static final int MIN_SECRET_BYTES = 32;
    // Valores que ja apareceram no repositorio e nunca podem ser usados.
    private static final Set<String> FORBIDDEN_SECRETS = Set.of("my-secret-key", "secretkey", "secret");

    @Value("${api.security.token.secret}")
    private String secret;

    @Value("${api.security.token.expiration-minutes:120}")
    private long expirationMinutes;

    private Algorithm algorithm;

    @PostConstruct
    void init() {
        if (secret == null
                || FORBIDDEN_SECRETS.contains(secret)
                || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente ou fraco: defina uma chave aleatoria com pelo menos 32 caracteres.");
        }
        this.algorithm = Algorithm.HMAC256(secret);
    }

    public String generateToken(User user) {
        try {
            return JWT.create()
                    .withIssuer(ISSUER)
                    .withSubject(user.getUsername())
                    .withIssuedAt(Instant.now())
                    .withExpiresAt(Instant.now().plus(Duration.ofMinutes(expirationMinutes)))
                    .sign(algorithm);
        } catch (JWTCreationException e) {
            throw new IllegalStateException("Error while generating token.", e);
        }
    }

    /** Retorna o login (subject) do token, ou null se o token for invalido ou estiver expirado. */
    public String validateToken(String token) {
        try {
            return JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .acceptLeeway(5)
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException e) {
            log.debug("Token JWT rejeitado: {}", e.getMessage());
            return null;
        }
    }
}
