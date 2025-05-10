package org.babynexign.babybilling.hrsservice.consumers;

import org.babynexign.babybilling.hrsservice.dto.CountTariffPaymentRequest;
import org.babynexign.babybilling.hrsservice.service.TariffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CountTariffPaymentRequestMessageConsumer {
    private final TariffService tariffService;

    @Autowired
    public CountTariffPaymentRequestMessageConsumer(TariffService tariffService) {
        this.tariffService = tariffService;
    }

    @Bean
    public Consumer<CountTariffPaymentRequest> countTariffPaymentRequestConsumer() {
        return tariffService::processCountTariffPaymentRequest;
    }
}
