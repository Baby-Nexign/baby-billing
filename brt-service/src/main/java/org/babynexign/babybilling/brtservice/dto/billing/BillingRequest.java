package org.babynexign.babybilling.brtservice.dto.billing;

public record BillingRequest(Long personId, Long tariffId, Long minutes, String callType, Boolean inOneNetwork) {
}
