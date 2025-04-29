package org.babynexign.babybilling.brtservice.dto;

import org.babynexign.babybilling.brtservice.entity.ExtraService;

import java.time.LocalDateTime;

public record ExtraServiceDTO(Long extraServiceId, LocalDateTime startDate) {

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
