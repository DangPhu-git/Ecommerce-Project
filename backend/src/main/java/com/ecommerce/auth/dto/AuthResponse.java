package com.ecommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned upon successful authentication.
 * Refresh token is omitted here as it is securely delivered via HttpOnly Cookie.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    @Builder.Default
    private long expiresIn = 900; // 15 mins in seconds

    private UserResponse user;
}
