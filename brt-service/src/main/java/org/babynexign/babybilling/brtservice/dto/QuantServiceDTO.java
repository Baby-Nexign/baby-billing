package org.babynexign.babybilling.brtservice.dto;

import org.babynexign.babybilling.brtservice.entity.QuantService;

public record QuantServiceDTO(String serviceType, Long amountLeft) {

    public static QuantServiceDTO fromEntity(QuantService entity) {
        if (entity == null) {
            return null;
        }

        return new QuantServiceDTO(
                entity.getServiceType().toString(),
                entity.getAmountLeft()
        );
    }
}
