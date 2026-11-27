package com.ecommerce.auth;

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
import com.ecommerce.auth.service.impl.AuthServiceImpl;
import com.ecommerce.common.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private IJwtService jwtService;

    @Mock
    private IRefreshTokenService refreshTokenService;

    @Mock
    private AuthenticationManager authenticationManager;

    private IAuthService authService;

    private User sampleUser;
    private RefreshToken sampleRefreshToken;
    private final org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder testEncoder =
            new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder(12);

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                authenticationManager
        );

        String validHash = testEncoder.encode("Secret123!");

        sampleUser = User.builder()
                .id(UUID.randomUUID())
                .email("alex@example.com")
                .firstName("Alex")
                .lastName("Smith")
                .passwordHash(validHash)
                .role(Role.ROLE_USER)
                .isActive(true)
                .createdAt(Instant.now())
                .build();

        sampleRefreshToken = RefreshToken.builder()
                .id(UUID.randomUUID())
                .user(sampleUser)
                .tokenHash("hash-value")
                .expiresAt(Instant.now().plusSeconds(604800))
                .build();
        sampleRefreshToken.setRawToken("raw-refresh-token-xyz");
    }

    @Test
    @DisplayName("register should succeed when email is available")
    void register_ShouldSucceed() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alex@example.com")
                .password("Secret123!")
                .firstName("Alex")
                .lastName("Smith")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenAnswer(inv -> testEncoder.encode(inv.getArgument(0)));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("sample.access.token");
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(sampleRefreshToken);

        AuthResult result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.getAuthResponse().getAccessToken()).isEqualTo("sample.access.token");
        assertThat(result.getAuthResponse().getUser().getEmail()).isEqualTo("alex@example.com");
        assertThat(result.getRawRefreshToken()).isEqualTo("raw-refresh-token-xyz");

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("register should reject duplicate email with EmailAlreadyExistsException")
    void register_ShouldThrowException_WhenEmailExists() {
        RegisterRequest request = RegisterRequest.builder()
                .email("alex@example.com")
                .password("Secret123!")
                .build();

        when(userRepository.existsByEmailIgnoreCase("alex@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login should authenticate credentials and issue tokens")
    void login_ShouldSucceed() {
        LoginRequest request = LoginRequest.builder()
                .email("alex@example.com")
                .password("Secret123!")
                .build();

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(sampleUser, null, sampleUser.getAuthorities());

        when(authenticationManager.authenticate(any())).thenReturn(authToken);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("sample.access.token");
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn(sampleRefreshToken);

        AuthResult result = authService.login(request);

        assertThat(result).isNotNull();
        assertThat(result.getAuthResponse().getAccessToken()).isEqualTo("sample.access.token");
        assertThat(result.getRawRefreshToken()).isEqualTo("raw-refresh-token-xyz");
    }

    @Test
    @DisplayName("refreshToken should verify and rotate token")
    void refreshToken_ShouldRotateToken() {
        String oldToken = "old-raw-token";
        when(refreshTokenService.verifyAndRotate(oldToken)).thenReturn(sampleRefreshToken);
        when(jwtService.generateAccessToken(sampleUser)).thenReturn("new.access.token");

        AuthResult result = authService.refreshToken(oldToken);

        assertThat(result.getAuthResponse().getAccessToken()).isEqualTo("new.access.token");
        assertThat(result.getRawRefreshToken()).isEqualTo("raw-refresh-token-xyz");
    }

    @Test
    @DisplayName("logout should call revokeToken on refreshTokenService")
    void logout_ShouldRevokeToken() {
        authService.logout("raw-token-to-revoke");
        verify(refreshTokenService, times(1)).revokeToken("raw-token-to-revoke");
    }

    @Test
    @DisplayName("getCurrentUser should return user details for existing user")
    void getCurrentUser_ShouldReturnProfile() {
        when(userRepository.findActiveByEmailIgnoreCase("alex@example.com")).thenReturn(Optional.of(sampleUser));

        UserResponse response = authService.getCurrentUser("alex@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("alex@example.com");
        assertThat(response.getFirstName()).isEqualTo("Alex");
    }
}
