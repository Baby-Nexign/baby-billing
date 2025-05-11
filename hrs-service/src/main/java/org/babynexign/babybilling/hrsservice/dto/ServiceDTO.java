package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.*;
import org.babynexign.babybilling.hrsservice.entity.OperatorService;

import java.time.LocalDate;

public record ServiceDTO(
        Long id, 
        
        @NotBlank(message = "Service name must not be empty")
        String name, 
        
        @NotBlank(message = "Service type must be specified")
        String serviceType, 
        
        @NotNull(message = "isQuantitative must not be empty")
        Boolean isQuantitative, 
        
        LocalDate startDate, 
        LocalDate avDate, 
        LocalDate acEndDate, 
        
        @Min(value = 0, message = "Amount cannot be negative")
        Long amount, 
        
        String description, 
        
        @Min(value = 0, message = "Cost cannot be negative")
        Long cost, 
        
        @Min(value = 0, message = "Period cannot be negative")
        Integer period
) {
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
