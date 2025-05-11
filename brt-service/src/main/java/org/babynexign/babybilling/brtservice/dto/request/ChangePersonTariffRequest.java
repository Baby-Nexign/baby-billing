package org.babynexign.babybilling.brtservice.dto.request;

import jakarta.validation.constraints.*;

public record ChangePersonTariffRequest(
        @NotNull(message = "New tariff ID must be specified")
        @Min(value = 1, message = "Tariff ID must be positive")
        Long newTariff
) {
}
