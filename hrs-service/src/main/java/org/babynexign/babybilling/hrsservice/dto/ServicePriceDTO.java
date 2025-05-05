package org.babynexign.babybilling.hrsservice.dto;

import org.babynexign.babybilling.hrsservice.entity.ServicePrice;

public record ServicePriceDTO(Long period, Long cost) {
    public static ServicePriceDTO fromEntity(ServicePrice entity) {
        if (entity == null) {
            return null;
        }

        return new ServicePriceDTO(
                entity.getPeriod(),
                entity.getCost()
        );
    }
}
