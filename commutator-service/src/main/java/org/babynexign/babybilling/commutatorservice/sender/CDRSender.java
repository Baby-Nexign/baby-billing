package org.babynexign.babybilling.commutatorservice.sender;

import org.babynexign.babybilling.commutatorservice.entity.Call;

import java.util.List;

/**
 * Interface for sending Call Detail Records (CDRs) to external systems (BRT in our case).
 */
public interface CDRSender {
    void sendCDR(List<Call> calls);
}
