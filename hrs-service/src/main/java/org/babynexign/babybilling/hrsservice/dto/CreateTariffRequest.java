package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateTariffRequest(
        @NotBlank(message = "Tariff name must not be empty")
        String name,
        
        @NotNull(message = "Payment period must be specified")
        @Min(value = 1, message = "Payment period must be positive")
        Integer paymentPeriod,
        
        @NotNull(message = "Cost must be specified")
        @Min(value = 0, message = "Cost cannot be negative")
        Long cost,
        
        String description,
        List<Long> serviceIds,
        CallPricesDTO callPrices
) {}
