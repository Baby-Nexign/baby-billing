package org.babynexign.babybilling.brtservice.senders.impl;

import org.babynexign.babybilling.brtservice.dto.commutator.CallRestrictionRequest;
import org.babynexign.babybilling.brtservice.dto.commutator.NewSubscriberRequest;
import org.babynexign.babybilling.brtservice.senders.CommutatorSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class RabbitCommutatorSender implements CommutatorSender {
    private final StreamBridge streamBridge;

    @Autowired
    public RabbitCommutatorSender(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void sendCallRestrictionRequest(CallRestrictionRequest callRestrictionRequest) {
        streamBridge.send("callRestrictionRequestProducer-out-0", callRestrictionRequest);
    }

    @Override
    public void sendNewSubscriberRequest(NewSubscriberRequest newSubscriberRequest) {
        streamBridge.send("newSubscriberRequestProducer-out-0", newSubscriberRequest);
    }
}
