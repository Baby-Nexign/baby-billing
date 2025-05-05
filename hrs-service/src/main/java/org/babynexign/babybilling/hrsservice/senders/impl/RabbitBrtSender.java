package org.babynexign.babybilling.hrsservice.senders.impl;

import org.babynexign.babybilling.hrsservice.dto.BillingResponse;
import org.babynexign.babybilling.hrsservice.dto.TariffInformationResponse;
import org.babynexign.babybilling.hrsservice.senders.BrtSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class RabbitBrtSender implements BrtSender {
    private final StreamBridge streamBridge;

    @Autowired
    public RabbitBrtSender(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void sendBillingResponse(BillingResponse billingResponse) {
        streamBridge.send("billingResponseProducer-out-0", billingResponse);
    }

    @Override
    public void sendTariffInformationResponse(TariffInformationResponse tariffInformationResponse) {
        streamBridge.send("tariffInformationResponseProducer-out-0", tariffInformationResponse);
    }
}
