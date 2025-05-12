package org.babynexign.babybilling.hrsservice.consumers;

import org.babynexign.babybilling.hrsservice.dto.BillingRequest;
import org.babynexign.babybilling.hrsservice.service.BillingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Consumer for billing request messages.
 * Processes billing calculations for call charges.
 */
@Configuration
public class BillingRequestMessageConsumer {
    private final BillingService billingService;

    @Autowired
    public BillingRequestMessageConsumer(BillingService billingService) {
        this.billingService = billingService;
    }


    @Bean
    public Consumer<BillingRequest> billingRequestConsumer() {
        return billingService::processBillingRequest;
    }
}
