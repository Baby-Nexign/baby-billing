package org.babynexign.babybilling.hrsservice.dto;

import jakarta.validation.constraints.*;
import org.babynexign.babybilling.hrsservice.entity.TelecomPrice;

public record TelecomPriceDTO(
        @NotBlank(message = "Telecom type must be specified")
        String telecomType, 
        
        @NotNull(message = "inOneNetwork must not be empty")
        Boolean inOurNetwork, 
        
        @NotBlank(message = "Telecom data type must be specified")
        String telecomDataType, 
        
        @Min(value = 0, message = "Cost cannot be negative")
        Long cost
) {
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
