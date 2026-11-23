package com.ecommerce.auth.security;

import com.ecommerce.common.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * AuthenticationEntryPoint invoked when an unauthenticated user attempts to access a protected resource.
 * Formats response as standardized ApiResponse error envelope with HTTP 401.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        String errorCode = "UNAUTHORIZED";
        String message = authException.getMessage();

        Boolean isExpired = (Boolean) request.getAttribute("token_expired");
        if (Boolean.TRUE.equals(isExpired)) {
            errorCode = "TOKEN_EXPIRED";
            message = "JWT token has expired";
        }

        log.debug("Authentication failed for URI [{}]: code={}, message={}",
                request.getRequestURI(), errorCode, message);

        ApiResponse<Void> apiResponse = ApiResponse.error(errorCode, message != null ? message : "Unauthorized access");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
