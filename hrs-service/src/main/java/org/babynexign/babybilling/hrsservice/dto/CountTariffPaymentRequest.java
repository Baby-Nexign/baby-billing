package org.babynexign.babybilling.hrsservice.dto;

import java.time.LocalDate;

public record CountTariffPaymentRequest(Long personId, Long tariffId, LocalDate startDate, LocalDate currentDate) {
}
