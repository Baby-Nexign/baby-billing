package org.babynexign.babybilling.hrsservice.dto;

public record CallPricesDTO(
        Long incomingInNetworkPrice,
        Long outgoingInNetworkPrice,
        Long incomingOutNetworkPrice,
        Long outgoingOutNetworkPrice
) {}
