package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.*;
import org.babynexign.babybilling.hrsservice.entity.Tariff;

import java.time.LocalDate;
import java.util.List;

public record TariffDTO(
        Long id, 
        
        @NotBlank(message = "Tariff name must not be empty")
        String name, 
        
        @NotNull(message = "Payment period must be specified")
        Integer paymentPeriod, 
        
        @Min(value = 0, message = "Cost cannot be negative") 
        Long cost, 
        
        LocalDate startDate, 
        LocalDate avEndDate, 
        LocalDate acEndDate, 
        String description, 
        List<ServiceDTO> services, 
        List<TelecomPriceDTO> telecomPrices
) {
    public static TariffDTO fromEntity(Tariff entity) {
        if (entity == null) {
            return null;
        }

        return new TariffDTO(
                entity.getId(),
                entity.getName(),
                entity.getPaymentPeriod(),
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
