package com.ecommerce.auth.service;

import com.ecommerce.auth.dto.AuthResult;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;

/**
 * Service contract for user registration, authentication, token rotation, and current user retrieval.
 */
public interface IAuthService {

    /**
     * Registers a new user with default ROLE_USER, hashes password, and issues initial access + refresh tokens.
     *
     * @param request registration payload
     * @return AuthResult containing client AuthResponse and raw refresh token
     */
    AuthResult register(RegisterRequest request);

    /**
     * Authenticates user credentials via AuthenticationManager, issuing access and refresh tokens.
     *
     * @param request login payload
     * @return AuthResult containing client AuthResponse and raw refresh token
     */
    AuthResult login(LoginRequest request);

    /**
     * Verifies the provided refresh token, executes rotation, and returns new token pair.
     *
     * @param rawRefreshToken plain-text refresh token from client
     * @return AuthResult with new access token and rotated refresh token
     */
    AuthResult refreshToken(String rawRefreshToken);

    /**
     * Revokes the provided refresh token session.
     *
     * @param rawRefreshToken plain-text refresh token to invalidate
     */
    void logout(String rawRefreshToken);

    /**
     * Retrieves current authenticated user profile by email.
     *
     * @param email user email
     * @return UserResponse DTO
     */
    UserResponse getCurrentUser(String email);
}
