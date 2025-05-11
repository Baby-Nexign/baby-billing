package org.babynexign.babybilling.hrsservice.dto;

import java.util.List;

public record CreateTariffRequest(
        String name,
        Integer paymentPeriod,
        Long cost,
        String description,
        List<Long> serviceIds,
        CallPricesDTO callPrices
) {}
