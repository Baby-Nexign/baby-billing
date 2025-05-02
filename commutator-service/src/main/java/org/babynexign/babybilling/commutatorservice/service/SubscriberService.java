package org.babynexign.babybilling.commutatorservice.service;

import org.babynexign.babybilling.commutatorservice.dto.requests.CallRestrictionRequest;
import org.babynexign.babybilling.commutatorservice.dto.requests.NewSubscriberRequest;
import org.babynexign.babybilling.commutatorservice.entity.Subscriber;
import org.babynexign.babybilling.commutatorservice.exception.SubscriberNotFoundException;
import org.babynexign.babybilling.commutatorservice.repository.SubscriberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubscriberService {
    private final SubscriberRepository subscriberRepository;

    @Autowired
    public SubscriberService(SubscriberRepository subscriberRepository) {
        this.subscriberRepository = subscriberRepository;
    }

    public void processNewSubscriberRequest(NewSubscriberRequest newSubscriberRequest) {
        Subscriber newSubscriber = Subscriber
                .builder()
                .msisdn(newSubscriberRequest.msisdn())
                .isRestricted(false)
                .build();

        subscriberRepository.save(newSubscriber);
    }

    public void processCallRestrictionRequest(CallRestrictionRequest callRestrictionRequest){
        Subscriber subscriber = subscriberRepository.findByMsisdn(callRestrictionRequest.subscriberMsisdn())
                .orElseThrow(() -> new SubscriberNotFoundException("Subscriber with MSISDN " + callRestrictionRequest.subscriberMsisdn() + " not found"));

        if (subscriber.getIsRestricted() != callRestrictionRequest.isRestricted()){
            subscriber.setIsRestricted(callRestrictionRequest.isRestricted());
        }
        subscriberRepository.save(subscriber);
    }
}
