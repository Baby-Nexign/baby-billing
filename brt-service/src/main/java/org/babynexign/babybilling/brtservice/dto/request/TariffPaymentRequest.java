package org.babynexign.babybilling.brtservice.dto.request;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record TariffPaymentRequest(
        @NotNull(message = "Current date must be specified")
        LocalDate currentDate
) {
}
