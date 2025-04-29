package org.babynexign.babybilling.brtservice.dto.request;

public record ChangePersonTariffRequest(String personMsisdn, Long newTariffId) {
}
