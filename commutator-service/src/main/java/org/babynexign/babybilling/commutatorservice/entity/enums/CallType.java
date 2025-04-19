package org.babynexign.babybilling.commutatorservice.entity.enums;

import lombok.Getter;

@Getter
public enum CallType {
    OUTCOMING("01"),
    INCOMING("02");

    private final String index;

    CallType(String index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return this.getIndex();
    }

}
