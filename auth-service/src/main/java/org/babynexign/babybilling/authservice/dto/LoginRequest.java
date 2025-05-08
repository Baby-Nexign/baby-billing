package org.babynexign.babybilling.authservice.dto;

public record LoginRequest(
        String login,
        String password
) {}
