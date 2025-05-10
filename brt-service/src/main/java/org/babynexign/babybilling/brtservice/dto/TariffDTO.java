package org.babynexign.babybilling.brtservice.dto;

import org.babynexign.babybilling.brtservice.entity.Tariff;

import java.time.LocalDate;

public record TariffDTO(Long tariffId, LocalDate startDate) {

    public static TariffDTO fromEntity(Tariff entity) {
        if (entity == null) {
            return null;
        }

        return new TariffDTO(
                entity.getTariffId(),
                entity.getStartDate()
        );
    }
}
