package org.babynexign.babybilling.brtservice.dto;

import org.babynexign.babybilling.brtservice.entity.ExtraService;

import java.time.LocalDate;

public record ExtraServiceDTO(Long extraServiceId, LocalDate startDate) {

    public static ExtraServiceDTO fromEntity(ExtraService entity) {
        if (entity == null) {
            return null;
        }

        return new ExtraServiceDTO(
                entity.getExtraServiceId(),
                entity.getStartDate()
        );
    }
}
