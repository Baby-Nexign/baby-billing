package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BillingRequest(
        @NotNull(message = "Person ID cannot be null")
        Long personId,
        
        @NotNull(message = "Tariff ID cannot be null")
        Long tariffId,
        
        @NotNull(message = "Minutes cannot be null")
        @Min(value = 0, message = "Minutes must be a non-negative value")
        Long minutes,
        
        @NotNull(message = "Call type cannot be null")
        String callType,
        
        @NotNull(message = "In-network flag cannot be null")
        Boolean inOneNetwork
) {}
