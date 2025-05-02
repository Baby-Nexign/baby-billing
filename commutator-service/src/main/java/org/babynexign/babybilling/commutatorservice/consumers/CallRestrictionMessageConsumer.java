package org.babynexign.babybilling.commutatorservice.consumers;

import org.babynexign.babybilling.commutatorservice.dto.requests.CallRestrictionRequest;
import org.babynexign.babybilling.commutatorservice.service.SubscriberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class CallRestrictionMessageConsumer {
    private final SubscriberService subscriberService;

    @Autowired
    public CallRestrictionMessageConsumer(SubscriberService subscriberService) {
        this.subscriberService = subscriberService;
    }

    @Bean
    public Consumer<CallRestrictionRequest> callRestrictionRequestConsumer() {
        return subscriberService::processCallRestrictionRequest;
    }
}
