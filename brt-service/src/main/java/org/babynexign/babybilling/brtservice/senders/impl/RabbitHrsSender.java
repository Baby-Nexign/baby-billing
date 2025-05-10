package org.babynexign.babybilling.brtservice.senders.impl;

import org.babynexign.babybilling.brtservice.dto.billing.BillingRequest;
import org.babynexign.babybilling.brtservice.dto.request.CountTariffPaymentRequest;
import org.babynexign.babybilling.brtservice.dto.request.TariffInformationRequest;
import org.babynexign.babybilling.brtservice.senders.HrsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Component;

@Component
public class RabbitHrsSender implements HrsSender {

    private final StreamBridge streamBridge;

    @Autowired
    public RabbitHrsSender(StreamBridge streamBridge) {
        this.streamBridge = streamBridge;
    }

    @Override
    public void sendBillingRequest(BillingRequest billingRequest) {
        streamBridge.send("billingRequestProducer-out-0", billingRequest);
    }

    @Override
    public void sendTariffInformationRequest(TariffInformationRequest tariffInformationRequest) {
        streamBridge.send("tariffInformationRequestProducer-out-0", tariffInformationRequest);
    }

    @Override
    public void sendCountTariffPaymentRequest(CountTariffPaymentRequest countTariffPaymentRequest) {
        streamBridge.send("countTariffPaymentRequestProducer-out-0", countTariffPaymentRequest);
    }
}
