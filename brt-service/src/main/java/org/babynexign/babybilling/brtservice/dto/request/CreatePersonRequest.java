package org.babynexign.babybilling.brtservice.dto.request;

import jakarta.validation.constraints.*;

public record CreatePersonRequest(
        @NotBlank(message = "Name must not be empty")
        String name, 
        
        @Pattern(regexp = "^[0-9]{11}$", message = "MSISDN must contain exactly 11 digits")
        String msisdn, 
        
        String description
) {
}
