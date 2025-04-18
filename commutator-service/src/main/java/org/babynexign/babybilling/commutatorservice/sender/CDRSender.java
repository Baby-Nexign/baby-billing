package org.babynexign.babybilling.commutatorservice.sender;

import org.babynexign.babybilling.commutatorservice.entity.Record;

import java.util.List;

public interface CDRSender {
    void sendCDR(List<Record> calls);
}
