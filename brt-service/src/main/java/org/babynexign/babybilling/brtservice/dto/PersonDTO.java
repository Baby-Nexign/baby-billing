package org.babynexign.babybilling.brtservice.dto;

import jakarta.validation.constraints.*;
import org.babynexign.babybilling.brtservice.entity.Person;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public record PersonDTO(
        @NotBlank(message = "Name must not be empty")
        String name,
        
        @Pattern(regexp = "^[0-9]{11}$", message = "MSISDN must contain exactly 11 digits")
        String msisdn,
        
        Long balance,
        
        @NotNull(message = "Restriction status must be specified")
        Boolean isRestricted,
        
        LocalDateTime registrationDate,
        String description,
        List<QuantServiceDTO> quantServices,
        TariffDTO tariff,
        List<ExtraServiceDTO> extraServices
) {

    public static PersonDTO fromEntity(Person entity) {
        if (entity == null) {
            return null;
        }

        TariffDTO tariffDTO = TariffDTO.fromEntity(entity.getTariff());

        List<QuantServiceDTO> quantServiceDTOs = entity.getQuantServices() != null ?
                entity.getQuantServices().stream()
                        .map(QuantServiceDTO::fromEntity)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        List<ExtraServiceDTO> extraServiceDTOs = entity.getExtraServices() != null ?
                entity.getExtraServices().stream()
                        .map(ExtraServiceDTO::fromEntity)
                        .collect(Collectors.toList()) :
                Collections.emptyList();

        return new PersonDTO(
                entity.getName(),
                entity.getMsisdn(),
                entity.getBalance(),
                entity.getIsRestricted(),
                entity.getRegistrationDate(),
                entity.getDescription(),
                quantServiceDTOs,
                tariffDTO,
                extraServiceDTOs
        );
    }
}
