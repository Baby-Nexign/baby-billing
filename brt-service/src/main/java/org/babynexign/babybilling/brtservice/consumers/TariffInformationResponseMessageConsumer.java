package org.babynexign.babybilling.brtservice.consumers;

import org.babynexign.babybilling.brtservice.dto.response.TariffInformationResponse;
import org.babynexign.babybilling.brtservice.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class TariffInformationResponseMessageConsumer {
    private final PersonService personService;

    @Autowired
    public TariffInformationResponseMessageConsumer(PersonService personService) {
        this.personService = personService;
    }

    @Bean
    public Consumer<TariffInformationResponse> tariffInformationResponseConsumer() {
        return personService::processChangePersonTariff;
    }
}
