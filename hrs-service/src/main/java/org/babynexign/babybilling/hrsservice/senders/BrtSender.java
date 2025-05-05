package org.babynexign.babybilling.hrsservice.senders;

import org.babynexign.babybilling.hrsservice.dto.BillingResponse;
import org.babynexign.babybilling.hrsservice.dto.TariffInformationResponse;

public interface BrtSender {
    void sendBillingResponse(BillingResponse billingResponse);
    void sendTariffInformationResponse(TariffInformationResponse tariffInformationResponse);
}
