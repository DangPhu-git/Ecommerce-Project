package com.ecommerce.auth.service;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Service contract for JWT generation, parsing, and claim extraction.
 */
public interface IJwtService {

    /**
     * Generates a signed JWT access token for the given user with default expiration (15 minutes).
     *
     * @param userDetails authenticated user details
     * @return signed JWT string
     */
    String generateAccessToken(UserDetails userDetails);

    /**
     * Generates a signed JWT access token with custom extra claims.
     *
     * @param extraClaims map of custom claims to include
     * @param userDetails authenticated user details
     * @return signed JWT string
     */
    String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails);

    /**
     * Extracts username (email) from the token subject.
     *
     * @param token JWT string
     * @return username / email
     */
    String extractUsername(String token);

    /**
     * Extracts the User ID UUID from token claims.
     *
     * @param token JWT string
     * @return User ID UUID or null if absent
     */
    UUID extractUserId(String token);

    /**
     * Extracts the user role string from token claims.
     *
     * @param token JWT string
     * @return user role name
     */
    String extractRole(String token);

    /**
     * Extracts token expiration timestamp.
     *
     * @param token JWT string
     * @return expiration Date
     */
    Date extractExpiration(String token);

    /**
     * Validates whether a token belongs to the given user and is not expired.
     *
     * @param token JWT string
     * @param userDetails user to validate against
     * @return true if valid
     */
    boolean isTokenValid(String token, UserDetails userDetails);

    /**
     * Checks if the token is past its expiration date.
     *
     * @param token JWT string
     * @return true if expired
     */
    boolean isTokenExpired(String token);
}
