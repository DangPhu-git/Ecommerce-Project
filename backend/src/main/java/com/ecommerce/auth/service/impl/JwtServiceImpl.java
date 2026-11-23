package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Implementation of JwtService using io.jsonwebtoken (JJWT 0.12.x).
 * Supports HMAC-SHA256 signing and verification.
 */
@Service
@Slf4j
public class JwtServiceImpl implements JwtService {

    private final String secretKey;
    private final long jwtExpirationInSeconds;

    public JwtServiceImpl(
            @Value("${app.jwt.secret:change-this-in-production-min-32-chars-long-secret-key-ecommerce}") String secretKey,
            @Value("${app.jwt.expiration:900}") long jwtExpirationInSeconds) {
        this.secretKey = secretKey;
        this.jwtExpirationInSeconds = jwtExpirationInSeconds;
    }

    @Override
    public String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(new HashMap<>(), userDetails);
    }

    @Override
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);

        if (userDetails instanceof User user) {
            if (user.getId() != null) {
                claims.putIfAbsent("userId", user.getId().toString());
            }
            if (user.getRole() != null) {
                claims.putIfAbsent("role", user.getRole().name());
            }
        }

        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtExpirationInSeconds);

        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey())
                .compact();
    }

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public UUID extractUserId(String token) {
        String userIdStr = extractClaim(token, claims -> claims.get("userId", String.class));
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    @Override
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            if (isTokenExpired(token)) {
                return false;
            }
            final String username = extractUsername(token);
            return username != null && username.equalsIgnoreCase(userDetails.getUsername());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            // Try decoding base64 if formatted as base64
            keyBytes = Decoders.BASE64.decode(secretKey);
            if (keyBytes.length < 32) {
                // If decoded bytes are too short, use direct UTF-8 bytes
                keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
