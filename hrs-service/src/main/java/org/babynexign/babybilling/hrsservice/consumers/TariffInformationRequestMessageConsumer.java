package org.babynexign.babybilling.hrsservice.consumers;

import org.babynexign.babybilling.hrsservice.dto.TariffInformationRequest;
import org.babynexign.babybilling.hrsservice.service.TariffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Consumer for tariff information request messages.
 * Retrieves and sends tariff details for subscribers.
 */
@Configuration
public class TariffInformationRequestMessageConsumer {
    private final TariffService tariffService;

    @Autowired
    public TariffInformationRequestMessageConsumer(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @Bean
    public Consumer<TariffInformationRequest> tariffInformationRequestConsumer() {
        return tariffService::processTariffInformationRequest;
    }
}
