package org.babynexign.babybilling.brtservice.dto.response;

import org.babynexign.babybilling.brtservice.dto.ExtraServiceDTO;
import org.babynexign.babybilling.brtservice.dto.QuantServiceDTO;

import java.util.List;

public record TariffInformationResponse(Long personId, Long tariffId, List<QuantServiceDTO> quantServices, List<ExtraServiceDTO> extraServices) {
}
