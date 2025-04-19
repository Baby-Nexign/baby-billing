package org.babynexign.babybilling.commutatorservice.sender;

import org.babynexign.babybilling.commutatorservice.entity.Call;

import java.util.List;

public interface CDRSender {
    void sendCDR(List<Call> calls);
}
