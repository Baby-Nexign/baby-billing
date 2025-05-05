package org.babynexign.babybilling.hrsservice.dto;

import java.util.List;

public record TariffInformationResponse(Long personId, Long tariffId, List<QuantServiceDTO> quantServices, List<ExtraServiceDTO> extraServices) {
}
