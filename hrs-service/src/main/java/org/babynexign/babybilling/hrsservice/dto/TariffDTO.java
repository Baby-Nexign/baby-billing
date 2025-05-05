package org.babynexign.babybilling.hrsservice.dto;

import org.babynexign.babybilling.hrsservice.entity.Tariff;

import java.time.LocalDateTime;
import java.util.List;

public record TariffDTO(Long id, String name, String tariffType, Long cost, LocalDateTime startDate, LocalDateTime avEndDate, LocalDateTime acEndDate, String description, List<ServiceDTO> services, List<TelecomPriceDTO> telecomPrices) {
    public static TariffDTO fromEntity(Tariff entity) {
        if (entity == null) {
            return null;
        }

        return new TariffDTO(
                entity.getId(),
                entity.getName(),
                entity.getType().getName().toString(),
                entity.getCost(),
                entity.getStartDate(),
                entity.getAvEndDate(),
                entity.getAcEndDate(),
                entity.getDescription(),
                entity.getServices().stream().map(ServiceDTO::fromEntity).toList(),
                entity.getTelecomPrices().stream().map(TelecomPriceDTO::fromEntity).toList()
        );
    }

}
