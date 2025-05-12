package org.babynexign.babybilling.commutatorservice.consumers;

import org.babynexign.babybilling.commutatorservice.dto.requests.NewSubscriberRequest;
import org.babynexign.babybilling.commutatorservice.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

/**
 * Consumer for new subscriber registration messages.
 * Handles creation of new subscriber records.
 */
@Configuration
public class NewSubscriberMessageConsumer {
    private final SubscriberService subscriberService;

    @Autowired
    public NewSubscriberMessageConsumer(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @Bean
    public Consumer<NewSubscriberRequest> newSubscriberRequestConsumer() {
        return subscriberService::processNewSubscriberRequest;
    }
}
