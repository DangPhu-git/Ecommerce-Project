package com.ecommerce.auth;

import com.ecommerce.auth.entity.Role;
import com.ecommerce.auth.entity.User;
import com.ecommerce.auth.security.JwtAuthFilter;
import com.ecommerce.auth.security.JwtAuthenticationEntryPoint;
import com.ecommerce.auth.service.IJwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private IJwtService jwtService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter jwtAuthFilter;
    private JwtAuthenticationEntryPoint entryPoint;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtAuthFilter = new JwtAuthFilter(jwtService, userDetailsService);
        objectMapper = new ObjectMapper().findAndRegisterModules();
        entryPoint = new JwtAuthenticationEntryPoint(objectMapper);
    }

    @Test
    @DisplayName("Should proceed without auth when Authorization header is missing")
    void doFilter_ShouldProceedWithoutAuth_WhenNoAuthHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Should authenticate user when valid Bearer token is provided")
    void doFilter_ShouldAuthenticate_WhenValidToken() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        when(jwtService.extractUsername("valid.jwt.token")).thenReturn("test@example.com");
        when(userDetailsService.loadUserByUsername("test@example.com")).thenReturn(user);
        when(jwtService.isTokenValid("valid.jwt.token", user)).thenReturn(true);

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("Should mark request as token_expired when ExpiredJwtException is thrown")
    void doFilter_ShouldFlagExpiredToken_WhenExpiredJwtException() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtService.extractUsername("expired.jwt.token"))
                .thenThrow(new ExpiredJwtException(null, null, "Token has expired"));

        jwtAuthFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute("token_expired")).isEqualTo(true);

        // Verify entry point handles the token_expired flag with TOKEN_EXPIRED error code
        entryPoint.commence(request, response, new BadCredentialsException("Token expired"));
        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("TOKEN_EXPIRED");
        assertThat(response.getContentAsString()).contains("JWT token has expired");
    }

    @Test
    @DisplayName("EntryPoint should return 401 UNAUTHORIZED for generic auth failure")
    void entryPoint_ShouldReturnUnauthorized_ForGenericFailure() throws IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new BadCredentialsException("Full authentication is required"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(response.getContentAsString()).contains("Full authentication is required");
    }
}
