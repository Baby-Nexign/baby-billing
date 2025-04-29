package org.babynexign.babybilling.brtservice.consumers;

import org.babynexign.babybilling.brtservice.dto.billing.BillingResponse;
import org.babynexign.babybilling.brtservice.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class BillingResponseMessageConsumer {

    private final PersonService personService;

    @Autowired
    public BillingResponseMessageConsumer(PersonService personService) {
        this.personService = personService;
    }

    @Bean
    public Consumer<BillingResponse> billingResponseConsumer() {
        return personService::processBillingResponse;
    }
}
