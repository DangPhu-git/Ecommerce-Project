package com.ecommerce.auth;

import com.ecommerce.auth.entity.RefreshToken;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.repository.RefreshTokenRepository;
import com.ecommerce.auth.repository.UserRepository;
import com.ecommerce.auth.service.IRefreshTokenService;
import com.ecommerce.auth.service.impl.RefreshTokenServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private UserRepository userRepository;

    private IRefreshTokenService refreshTokenService;

    private User testUser;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenServiceImpl(refreshTokenRepository, userRepository, 604800L);

        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("user@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("createRefreshToken should store SHA-256 hash and return raw token")
    void createRefreshToken_ShouldStoreHashAndReturnRawToken() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken result = refreshTokenService.createRefreshToken(testUser);

        assertThat(result).isNotNull();
        assertThat(result.getRawToken()).isNotBlank();
        assertThat(result.getTokenHash()).isNotBlank();
        assertThat(result.isRevoked()).isFalse();
        assertThat(result.getExpiresAt()).isAfter(Instant.now());
        assertThat(result.getTokenHash()).isEqualTo(refreshTokenService.hashToken(result.getRawToken()));

        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("verifyAndRotate should revoke old token and issue fresh token on valid use")
    void verifyAndRotate_ShouldRotateTokens_WhenValid() {
        String rawToken = "sample-raw-refresh-token-12345";
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken existingToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(existingToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = refreshTokenService.verifyAndRotate(rawToken);

        // Verify existing token was revoked
        assertThat(existingToken.isRevoked()).isTrue();
        // Verify a new token was generated
        assertThat(rotated).isNotNull();
        assertThat(rotated.getRawToken()).isNotBlank();
        assertThat(rotated.isRevoked()).isFalse();
    }

    @Test
    @DisplayName("verifyAndRotate should detect reuse, revoke ALL user sessions, and throw SecurityException")
    void verifyAndRotate_ShouldTriggerReuseDetection_WhenTokenAlreadyRevoked() {
        String rawToken = "compromised-reused-token";
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken revokedToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true) // ALREADY REVOKED!
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.verifyAndRotate(rawToken))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Refresh token reuse detected");

        // Verify all user tokens were revoked across all sessions
        verify(refreshTokenRepository, times(1)).revokeAllByUserId(testUser.getId());
    }

    @Test
    @DisplayName("verifyAndRotate should reject expired tokens")
    void verifyAndRotate_ShouldReject_WhenExpired() {
        String rawToken = "expired-token";
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken expiredToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().minusSeconds(3600)) // EXPIRED
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.verifyAndRotate(rawToken))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("expired");

        assertThat(expiredToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(expiredToken);
    }

    @Test
    @DisplayName("revokeToken should mark token as revoked")
    void revokeToken_ShouldRevokeMatchingToken() {
        String rawToken = "token-to-revoke";
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken activeToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .tokenHash(tokenHash)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(activeToken));

        refreshTokenService.revokeToken(rawToken);

        assertThat(activeToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(activeToken);
    }

    @Test
    @DisplayName("revokeAllUserTokens should invoke repository to revoke all tokens for user")
    void revokeAllUserTokens_ShouldInvokeRepository() {
        UUID userId = UUID.randomUUID();
        refreshTokenService.revokeAllUserTokens(userId);

        verify(refreshTokenRepository, times(1)).revokeAllByUserId(userId);
    }
}
