package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.RefreshTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Implementation of RefreshTokenService.
 * Enforces SHA-256 hashed storage, token rotation on every use,
 * and automatic session revocation upon reuse detection.
 */
@Service
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final long refreshExpirationInSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.jwt.refresh-expiration:604800}") long refreshExpirationInSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.refreshExpirationInSeconds = refreshExpirationInSeconds;
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));
        return createRefreshToken(user);
    }

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user) {
        String rawToken = generateSecureTokenString();
        String tokenHash = hashToken(rawToken);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(refreshExpirationInSeconds))
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        saved.setRawToken(rawToken);
        return saved;
    }

    @Override
    @Transactional
    public RefreshToken verifyAndRotate(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        // Reuse detection: If a revoked token is presented, compromise is assumed
        if (token.isRevoked()) {
            UUID userId = token.getUser().getId();
            log.warn("Security Alert: Refresh token reuse detected for user ID: {}! Invalidating all user sessions.", userId);
            revokeAllUserTokens(userId);
            throw new SecurityException("Refresh token reuse detected. All sessions have been revoked for security.");
        }

        // Expiration check
        if (token.isExpired()) {
            token.revoke();
            refreshTokenRepository.save(token);
            throw new BadCredentialsException("Refresh token has expired");
        }

        // Token rotation: Revoke current token and generate a new one
        token.revoke();
        refreshTokenRepository.save(token);

        return createRefreshToken(token.getUser());
    }

    @Override
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.revoke();
            refreshTokenRepository.save(token);
        });
    }

    @Override
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    @Override
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private String generateSecureTokenString() {
        byte[] randomBytes = new byte[48];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
