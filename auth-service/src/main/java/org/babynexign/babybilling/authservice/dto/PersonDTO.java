package org.babynexign.babybilling.authservice.dto;

import org.babynexign.babybilling.authservice.dto.palceholders.ExtraServiceDTO;
import org.babynexign.babybilling.authservice.dto.palceholders.QuantServiceDTO;
import org.babynexign.babybilling.authservice.dto.palceholders.TariffDTO;

import java.time.LocalDateTime;
import java.util.List;

public record PersonDTO(
        String name,
        String msisdn,
        Long balance,
        Boolean isRestricted,
        LocalDateTime registrationDate,
        String description,
        List<QuantServiceDTO> quantServices,
        TariffDTO tariff,
        List<ExtraServiceDTO> extraServices
) {}
