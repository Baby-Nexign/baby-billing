package org.babynexign.babybilling.authservice.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(
        @NotBlank(message = "Login must not be empty")
        String login,
        
        @NotBlank(message = "Password must not be empty")
        String password
) {}
