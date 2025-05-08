package org.babynexign.babybilling.authservice.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken
) { }
