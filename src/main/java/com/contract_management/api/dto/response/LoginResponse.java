package com.contract_management.api.dto.response;

public record LoginResponse(
        String token,
        String tipo,
        long expiresIn,
        String papel
) {
}
