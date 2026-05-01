package com.finpred.auth.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * Utilitário para geração e validação de tokens JWT.
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    /**
     * Gera um token JWT para o usuário.
     */
    public String generateToken(Long userId, String email, Map<String, Object> extraClaims) {
        SecretKey key = getSigningKey();

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claims(extraClaims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(key)
                .compact();
    }

    /**
     * Valida e extrai claims de um token JWT.
     */
    public Claims validateToken(String token) {
        SecretKey key = getSigningKey();
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extrai o userId (subject) de um token.
     */
    public Long extractUserId(String token) {
        return Long.valueOf(validateToken(token).getSubject());
    }

    /**
     * Verifica se o token está expirado.
     */
    public boolean isTokenExpired(String token) {
        return validateToken(token).getExpiration().before(new Date());
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }
}
