package org.babynexign.babybilling.brtservice.consumers;

import org.babynexign.babybilling.brtservice.dto.response.CountTariffPaymentResponse;
import org.babynexign.babybilling.brtservice.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CountTariffPaymentMessageConsumer {
    private final PersonService personService;

    @Autowired
    public CountTariffPaymentMessageConsumer(PersonService personService) {
        this.personService = personService;
    }

    @Bean
    public Consumer<CountTariffPaymentResponse> countTariffPaymentResponseConsumer() {
        return personService::processCountTariffPaymentResponse;
    }
}
