package org.babynexign.babybilling.hrsservice.service;

import org.babynexign.babybilling.hrsservice.dto.BillingRequest;
import org.babynexign.babybilling.hrsservice.dto.BillingResponse;
import org.babynexign.babybilling.hrsservice.entity.Tariff;
import org.babynexign.babybilling.hrsservice.entity.TelecomPrice;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomDataTypeName;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomTypeName;
import org.babynexign.babybilling.hrsservice.repository.TariffRepository;
import org.babynexign.babybilling.hrsservice.senders.BrtSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BillingService {
    private final BrtSender brtSender;
    private final TariffRepository tariffRepository;

    @Autowired
    public BillingService(BrtSender brtSender, TariffRepository tariffRepository) {
        this.brtSender = brtSender;
        this.tariffRepository = tariffRepository;
    }

    @Transactional
    public void processBillingRequest(BillingRequest billingRequest) {
        Tariff subscriberTariff = tariffRepository.findById(billingRequest.tariffId()).orElseThrow(); // TODO: throw ex

        TelecomPrice price = subscriberTariff.getTelecomPrices().stream()
                .filter(telecomPrice ->
                        telecomPrice.getTelecomType().getName() == TelecomTypeName.valueOf(billingRequest.callType()) &&
                                telecomPrice.getInOurNetwork().equals(billingRequest.inOneNetwork()) &&
                                telecomPrice.getTelecomDataType().getName() == TelecomDataTypeName.MINUTES
                )
                .findFirst()
                .orElseThrow(); // TODO: throw ex

        Long finalCost = price.getCost() * billingRequest.minutes();

        brtSender.sendBillingResponse(new BillingResponse(billingRequest.personId(), finalCost));
    }
}
