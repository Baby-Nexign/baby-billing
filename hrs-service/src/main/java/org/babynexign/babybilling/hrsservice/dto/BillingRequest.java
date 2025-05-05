package org.babynexign.babybilling.hrsservice.dto;

public record BillingRequest(Long personId, Long tariffId, Long minutes, String callType, Boolean inOneNetwork) {
}
