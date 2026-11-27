package com.ecommerce.auth;

import com.ecommerce.auth.controller.AuthController;
import com.ecommerce.auth.dto.AuthResponse;
import com.ecommerce.auth.dto.AuthResult;
import com.ecommerce.auth.dto.LoginRequest;
import com.ecommerce.auth.dto.RegisterRequest;
import com.ecommerce.auth.dto.UserResponse;
import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.service.IAuthService;
import com.ecommerce.auth.service.IJwtService;
import com.ecommerce.common.exception.EmailAlreadyExistsException;
import com.ecommerce.common.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private IAuthService authService;

    @Mock
    private IJwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private UserResponse testUserDto;
    private AuthResult testAuthResult;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testUserDto = UserResponse.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .role(Role.ROLE_USER)
                .active(true)
                .createdAt(Instant.now())
                .build();

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken("access.jwt.token")
                .tokenType("Bearer")
                .expiresIn(900)
                .user(testUserDto)
                .build();

        testAuthResult = AuthResult.builder()
                .authResponse(authResponse)
                .rawRefreshToken("test-refresh-cookie-value")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 Created and set HttpOnly cookie")
    void register_ShouldReturnCreated() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(testAuthResult);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(cookie().httpOnly("refreshToken", true))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"))
                .andExpect(jsonPath("$.data.user.email").value("test@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with weak password should return 400 VALIDATION_ERROR")
    void register_WithWeakPassword_ShouldReturnBadRequest() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("test@example.com")
                .password("simple") // Too short and missing uppercase/special
                .firstName("Test")
                .lastName("User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.details", notNullValue()));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with existing email should return 409 EMAIL_ALREADY_EXISTS")
    void register_WithExistingEmail_ShouldReturnConflict() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("taken@example.com")
                .password("Password123!")
                .firstName("Test")
                .lastName("User")
                .build();

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email is already registered: taken@example.com"));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.error.message").value(containsString("already registered")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK and set HttpOnly cookie")
    void login_ShouldReturnOk() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(testAuthResult);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid credentials should return 401 INVALID_CREDENTIALS")
    void login_WithBadCredentials_ShouldReturnUnauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("test@example.com")
                .password("WrongPassword")
                .build();

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh should rotate token and return 200 OK")
    void refresh_ShouldRotateToken() throws Exception {
        when(authService.refreshToken("test-refresh-cookie-value")).thenReturn(testAuthResult);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie("refreshToken", "test-refresh-cookie-value")))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refreshToken"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("access.jwt.token"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh without cookie or body should return 401")
    void refresh_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout should clear refreshToken cookie")
    void logout_ShouldClearCookie() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .cookie(new Cookie("refreshToken", "active-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).logout("active-refresh-token");
    }

    @Test
    @DisplayName("GET /api/v1/auth/me with principal should return 200 and profile")
    void getMe_WithPrincipal_ShouldReturnProfile() throws Exception {
        when(authService.getCurrentUser("test@example.com")).thenReturn(testUserDto);

        mockMvc.perform(get("/api/v1/auth/me")
                        .principal(new org.springframework.security.authentication.TestingAuthenticationToken("test@example.com", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.firstName").value("Test"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me without principal should return 401")
    void getMe_WithoutPrincipal_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }
}
