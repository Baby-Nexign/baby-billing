package org.babynexign.babybilling.hrsservice.dto;

import org.babynexign.babybilling.hrsservice.entity.OperatorService;

import java.time.LocalDate;

public record ServiceDTO(Long id, String name, String serviceType, Boolean isQuantitative, LocalDate startDate, LocalDate avDate, LocalDate acEndDate, Long amount, String description, Long cost, Integer period) {
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
                entity.getCost(),
                entity.getPeriod()
        );
    }
}
