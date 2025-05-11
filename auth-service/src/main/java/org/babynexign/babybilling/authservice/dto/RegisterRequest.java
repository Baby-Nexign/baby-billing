package org.babynexign.babybilling.authservice.dto;

import jakarta.validation.constraints.*;
import java.util.Set;

public record RegisterRequest(
        String username,
        
        @Pattern(regexp = "^[0-9]{11}$", message = "MSISDN must contain exactly 11 digits")
        String msisdn,
        
        @NotBlank(message = "Password must not be empty")
        String password,
        
        Set<String> roles
) {}
