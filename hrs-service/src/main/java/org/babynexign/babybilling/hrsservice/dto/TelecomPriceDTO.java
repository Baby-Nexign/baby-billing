package org.babynexign.babybilling.hrsservice.dto;

import org.babynexign.babybilling.hrsservice.entity.TelecomPrice;

public record TelecomPriceDTO(String telecomType, Boolean inOurNetwork, String telecomDataType, Long cost) {
    public static TelecomPriceDTO fromEntity(TelecomPrice entity) {
        if (entity == null) {
            return null;
        }

        return new TelecomPriceDTO(
                entity.getTelecomType().getName().toString(),
                entity.getInOurNetwork(),
                entity.getTelecomDataType().getName().toString(),
                entity.getCost()
        );
    }

}
