package org.babynexign.babybilling.brtservice.dto.request;

import java.time.LocalDate;

public record TariffPaymentRequest(LocalDate currentDate) {
}
