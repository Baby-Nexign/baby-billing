package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.NotNull;

public record TariffInformationRequest(
    @NotNull(message = "Person ID cannot be null")
    Long personId,
    
    @NotNull(message = "Tariff ID cannot be null")
    Long tariffId
) {}
