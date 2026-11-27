package com.ecommerce.auth.service.impl;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.AuthResult;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.IAuthService;
import com.ecommerce.auth.service.IJwtService;
import com.ecommerce.auth.service.IRefreshTokenService;
import com.ecommerce.common.exception.EmailAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of IAuthService providing secure registration, login, token refresh rotation,
 * and profile retrieval.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final IRefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResult register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException("Email is already registered: " + normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);
        log.info("Successfully registered new user with ID: {}", savedUser.getId());

        String accessToken = jwtService.generateAccessToken(savedUser);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(mapToUserResponse(savedUser))
                .build();

        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(refreshToken.getRawToken())
                .build();
    }

    @Override
    @Transactional
    public AuthResult login(LoginRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedEmail, request.getPassword())
        );

        User user;
        if (authentication.getPrincipal() instanceof User authenticatedUser) {
            user = authenticatedUser;
        } else {
            user = userRepository.findActiveByEmailIgnoreCase(normalizedEmail)
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        }

        log.debug("User authenticated successfully: {}", user.getEmail());

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(mapToUserResponse(user))
                .build();

        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(refreshToken.getRawToken())
                .build();
    }

    @Override
    @Transactional
    public AuthResult refreshToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required");
        }

        RefreshToken rotatedToken = refreshTokenService.verifyAndRotate(rawRefreshToken);
        User user = rotatedToken.getUser();

        String newAccessToken = jwtService.generateAccessToken(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(900)
                .user(mapToUserResponse(user))
                .build();

        return AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken(rotatedToken.getRawToken())
                .build();
    }

    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokenService.revokeToken(rawRefreshToken);
            log.debug("Revoked refresh token upon logout");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        User user = userRepository.findActiveByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("User not found or account is deactivated"));
        return mapToUserResponse(user);
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
