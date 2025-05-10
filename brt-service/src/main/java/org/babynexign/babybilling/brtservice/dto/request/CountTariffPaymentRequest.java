package org.babynexign.babybilling.brtservice.dto.request;

import java.time.LocalDate;

public record CountTariffPaymentRequest(Long personId, Long tariffId, LocalDate startDate, LocalDate currentDate) {
}
