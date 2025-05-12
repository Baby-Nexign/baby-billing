package org.babynexign.babybilling.hrsservice.service;

import org.babynexign.babybilling.hrsservice.dto.BillingRequest;
import org.babynexign.babybilling.hrsservice.dto.BillingResponse;
import org.babynexign.babybilling.hrsservice.entity.Tariff;
import org.babynexign.babybilling.hrsservice.entity.TelecomPrice;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomDataTypeName;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomTypeName;
import org.babynexign.babybilling.hrsservice.exception.BillingValidationException;
import org.babynexign.babybilling.hrsservice.exception.TariffNotFoundException;
import org.babynexign.babybilling.hrsservice.exception.TelecomPriceNotFoundException;
import org.babynexign.babybilling.hrsservice.repository.TariffRepository;
import org.babynexign.babybilling.hrsservice.senders.BrtSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for processing billing requests and calculating call costs.
 */
@Service
public class BillingService {
    private final BrtSender brtSender;
    private final TariffRepository tariffRepository;

    @Autowired
    public BillingService(BrtSender brtSender, TariffRepository tariffRepository) {
        this.brtSender = brtSender;
        this.tariffRepository = tariffRepository;
    }

    /**
     * Processes a billing request by calculating the cost based on tariff and telecom price.
     *
     * @param billingRequest The request containing billing information
     * @throws BillingValidationException if the request contains invalid data
     * @throws TariffNotFoundException if the tariff is not found
     * @throws TelecomPriceNotFoundException if no matching telecom price is found for the given criteria
     */
    @Transactional
    public void processBillingRequest(BillingRequest billingRequest) {
        TelecomTypeName telecomTypeName;
        try {
            telecomTypeName = TelecomTypeName.valueOf(billingRequest.callType());
        } catch (IllegalArgumentException e) {
            throw new BillingValidationException("Invalid call type: " + billingRequest.callType(), e);
        }

        if (billingRequest.minutes() == null || billingRequest.minutes() < 0) {
            throw new BillingValidationException("Minutes must be a non-negative value");
        }

        Tariff subscriberTariff = tariffRepository.findById(billingRequest.tariffId())
                .orElseThrow(() -> new TariffNotFoundException(
                        "Tariff with ID " + billingRequest.tariffId() + " not found"));

        TelecomPrice price = subscriberTariff.getTelecomPrices().stream()
                .filter(telecomPrice ->
                        telecomPrice.getTelecomType().getName() == telecomTypeName &&
                        telecomPrice.getInOurNetwork().equals(billingRequest.inOneNetwork()) &&
                        telecomPrice.getTelecomDataType().getName() == TelecomDataTypeName.MINUTES
                )
                .findFirst()
                .orElseThrow(() -> new TelecomPriceNotFoundException(
                        "No telecom price found for call type: " + billingRequest.callType() +
                        ", in network: " + billingRequest.inOneNetwork() +
                        ", data type: MINUTES"));

        Long finalCost = price.getCost() * billingRequest.minutes();

        brtSender.sendBillingResponse(new BillingResponse(billingRequest.personId(), finalCost));
    }
}
