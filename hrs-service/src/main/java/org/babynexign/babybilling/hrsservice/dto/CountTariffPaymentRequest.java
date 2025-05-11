package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CountTariffPaymentRequest(
    @NotNull(message = "Person ID cannot be null")
    Long personId, 
    
    @NotNull(message = "Tariff ID cannot be null")
    Long tariffId, 
    
    @NotNull(message = "Start date cannot be null")
    LocalDate startDate, 
    
    @NotNull(message = "Current date cannot be null")
    LocalDate currentDate
) {}
