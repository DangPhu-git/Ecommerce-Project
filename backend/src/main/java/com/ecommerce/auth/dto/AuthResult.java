package com.ecommerce.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Internal result transfer object pairing the client-facing AuthResponse with the raw refresh token for cookie attachment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {

    private AuthResponse authResponse;
    private String rawRefreshToken;
}
