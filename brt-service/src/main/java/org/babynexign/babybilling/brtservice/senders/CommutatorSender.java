package org.babynexign.babybilling.brtservice.senders;

import org.babynexign.babybilling.brtservice.dto.commutator.CallRestrictionRequest;
import org.babynexign.babybilling.brtservice.dto.commutator.NewSubscriberRequest;

public interface CommutatorSender {
    void sendCallRestrictionRequest(CallRestrictionRequest callRestrictionRequest);
    void sendNewSubscriberRequest(NewSubscriberRequest newSubscriberRequest);
}
