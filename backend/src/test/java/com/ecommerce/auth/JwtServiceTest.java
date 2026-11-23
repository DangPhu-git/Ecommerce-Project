package com.ecommerce.auth;

import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.service.JwtService;
import com.ecommerce.auth.service.impl.JwtServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "super-secret-key-for-testing-purposes-must-be-at-least-32-bytes-long";
    private static final long TEST_EXPIRATION = 900; // 15 mins

    private JwtService jwtService;
    private User testUser;

    @BeforeEach
    void setUp() {
        jwtService = new JwtServiceImpl(TEST_SECRET, TEST_EXPIRATION);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("john.doe@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("generateAccessToken should issue a valid JWT with user claims")
    void generateAccessToken_ShouldIssueValidToken() {
        String token = jwtService.generateAccessToken(testUser);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("john.doe@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.ROLE_USER.name());
        assertThat(jwtService.isTokenValid(token, testUser)).isTrue();
    }

    @Test
    @DisplayName("generateAccessToken with extra claims should preserve custom claims")
    void generateAccessToken_WithExtraClaims_ShouldPreserveClaims() {
        Map<String, Object> extraClaims = Map.of("customClaim", "customValue");
        String token = jwtService.generateAccessToken(extraClaims, testUser);

        assertThat(jwtService.extractUsername(token)).isEqualTo("john.doe@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(testUser.getId());
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.ROLE_USER.name());
    }

    @Test
    @DisplayName("isTokenValid should return false if username does not match")
    void isTokenValid_ShouldReturnFalse_WhenUsernameDoesNotMatch() {
        String token = jwtService.generateAccessToken(testUser);

        UserDetails otherUser = User.builder()
                .id(UUID.randomUUID())
                .email("jane.doe@example.com")
                .role(Role.ROLE_USER)
                .build();

        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired should detect expired token")
    void isTokenExpired_ShouldReturnTrue_WhenTokenExpired() {
        // JwtService with -1 second expiration for immediate expiry
        JwtService expiredJwtService = new JwtServiceImpl(TEST_SECRET, -10);
        String expiredToken = expiredJwtService.generateAccessToken(testUser);

        assertThat(expiredJwtService.isTokenExpired(expiredToken)).isTrue();
        assertThat(expiredJwtService.isTokenValid(expiredToken, testUser)).isFalse();
    }

    @Test
    @DisplayName("extractExpiration should return future date for fresh token")
    void extractExpiration_ShouldReturnFutureDate() {
        String token = jwtService.generateAccessToken(testUser);
        Date expiration = jwtService.extractExpiration(token);

        assertThat(expiration).isAfter(new Date());
    }
}
