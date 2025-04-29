package org.babynexign.babybilling.brtservice.senders;

import org.babynexign.babybilling.brtservice.dto.billing.BillingRequest;
import org.babynexign.babybilling.brtservice.dto.request.TariffInformationRequest;

public interface HrsSender {
    void sendBillingRequest(BillingRequest billingRequest);
    void sendTariffInformationRequest(TariffInformationRequest tariffInformationRequest);
}
