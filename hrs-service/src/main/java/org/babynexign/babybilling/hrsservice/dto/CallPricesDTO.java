package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.*;

public record CallPricesDTO(
        @NotNull(message = "Incoming in-network price must be specified")
        @Min(value = 0, message = "Price cannot be negative")
        Long incomingInNetworkPrice,
        
        @NotNull(message = "Outgoing in-network price must be specified")
        @Min(value = 0, message = "Price cannot be negative")
        Long outgoingInNetworkPrice,
        
        @NotNull(message = "Incoming out-network price must be specified")
        @Min(value = 0, message = "Price cannot be negative")
        Long incomingOutNetworkPrice,
        
        @NotNull(message = "Outgoing out-network price must be specified")
        @Min(value = 0, message = "Price cannot be negative")
        Long outgoingOutNetworkPrice
) {}
