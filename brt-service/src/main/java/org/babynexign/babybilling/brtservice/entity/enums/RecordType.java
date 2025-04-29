package org.babynexign.babybilling.brtservice.entity.enums;

import lombok.Getter;

@Getter
public enum RecordType {
    OUTCOMING("01"),
    INCOMING("02");

    private final String index;

    RecordType(String index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return this.getIndex();
    }
}