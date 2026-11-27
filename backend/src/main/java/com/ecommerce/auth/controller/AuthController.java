package com.ecommerce.auth.controller;

import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.AuthResult;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.service.IAuthService;
import com.ecommerce.common.dto.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST Controller exposing authentication endpoints: registration, login, token refresh, logout, and profile.
 * Delivers refresh tokens securely via HttpOnly, SameSite=Strict cookies.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String LEGACY_COOKIE_NAME = "refresh_token";
    private static final long REFRESH_TOKEN_MAX_AGE = 7 * 24 * 60 * 60L; // 7 days in seconds

    private final IAuthService authService;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean cookieSecure;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResult result = authService.register(request);
        ResponseCookie cookie = buildRefreshTokenCookie(result.getRawRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.getAuthResponse(), "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResult result = authService.login(request);
        ResponseCookie cookie = buildRefreshTokenCookie(result.getRawRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.getAuthResponse(), "Logged in successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> body) {

        String rawToken = extractRefreshToken(request, body);
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadCredentialsException("Refresh token is required via cookie or request body");
        }

        AuthResult result = authService.refreshToken(rawToken);
        ResponseCookie cookie = buildRefreshTokenCookie(result.getRawRefreshToken(), REFRESH_TOKEN_MAX_AGE);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(result.getAuthResponse(), "Token refreshed successfully"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> body) {

        String rawToken = extractRefreshToken(request, body);
        if (rawToken != null && !rawToken.isBlank()) {
            authService.logout(rawToken);
        }

        ResponseCookie cleanCookie = buildRefreshTokenCookie("", 0);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cleanCookie.toString())
                .body(ApiResponse.success(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(java.security.Principal principal) {

        if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
            throw new BadCredentialsException("Authentication required to access profile");
        }

        UserResponse userResponse = authService.getCurrentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(userResponse));
    }

    private String extractRefreshToken(HttpServletRequest request, Map<String, String> body) {
        // 1. Try extracting from cookies
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie c : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(c.getName()) || LEGACY_COOKIE_NAME.equals(c.getName())) {
                    if (c.getValue() != null && !c.getValue().isBlank()) {
                        return c.getValue();
                    }
                }
            }
        }

        // 2. Fallback to request body if present
        if (body != null) {
            if (body.containsKey(REFRESH_TOKEN_COOKIE_NAME) && body.get(REFRESH_TOKEN_COOKIE_NAME) != null) {
                return body.get(REFRESH_TOKEN_COOKIE_NAME);
            }
            if (body.containsKey(LEGACY_COOKIE_NAME) && body.get(LEGACY_COOKIE_NAME) != null) {
                return body.get(LEGACY_COOKIE_NAME);
            }
        }

        return null;
    }

    private ResponseCookie buildRefreshTokenCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
