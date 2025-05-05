package org.babynexign.babybilling.hrsservice.dto;

import org.babynexign.babybilling.hrsservice.entity.OperatorService;

import java.time.LocalDateTime;
import java.util.List;

public record ServiceDTO(Long id, String name, String serviceType, Boolean isQuantitative, LocalDateTime startDate, LocalDateTime avDate, LocalDateTime acEndDate, Long amount, String description, List<ServicePriceDTO> servicePrices) {
    public static ServiceDTO fromEntity(OperatorService entity) {
        if (entity == null) {
            return null;
        }

        return new ServiceDTO(
                entity.getId(),
                entity.getName(),
                entity.getServiceType().getName().toString(),
                entity.getIsQuantitative(),
                entity.getStartDate(),
                entity.getAvDate(),
                entity.getAcEndDate(),
                entity.getAmount(),
                entity.getDescription(),
                entity.getServicePrices().stream().map(ServicePriceDTO::fromEntity).toList()
        );
    }
}
