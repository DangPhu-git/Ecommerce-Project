package com.ecommerce.auth.service;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.User;

import java.util.UUID;

/**
 * Service contract for RefreshToken operations, rotation, and reuse detection.
 */
public interface IRefreshTokenService {

    /**
     * Issues a new refresh token for a user by user ID.
     *
     * @param userId ID of the user
     * @return RefreshToken entity populated with rawToken in transient field
     */
    RefreshToken createRefreshToken(UUID userId);

    /**
     * Issues a new refresh token for a user entity.
     *
     * @param user User entity
     * @return RefreshToken entity populated with rawToken in transient field
     */
    RefreshToken createRefreshToken(User user);

    /**
     * Verifies the presented raw refresh token and rotates it:
     * - If revoked, triggers reuse detection and revokes ALL tokens for the user.
     * - If expired, revokes token and rejects.
     * - If valid, revokes current token and issues a new refresh token.
     *
     * @param rawRefreshToken plain-text token received from client
     * @return freshly issued RefreshToken entity
     */
    RefreshToken verifyAndRotate(String rawRefreshToken);

    /**
     * Explicitly revokes a single refresh token (e.g. on logout).
     *
     * @param rawRefreshToken plain-text token to revoke
     */
    void revokeToken(String rawRefreshToken);

    /**
     * Revokes all active refresh tokens for the given user.
     *
     * @param userId user ID
     */
    void revokeAllUserTokens(UUID userId);

    /**
     * Computes the SHA-256 hash of a raw token string for secure database lookup.
     *
     * @param rawToken plain-text token
     * @return hex-encoded SHA-256 hash
     */
    String hashToken(String rawToken);
}
