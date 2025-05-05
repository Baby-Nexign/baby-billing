package org.babynexign.babybilling.hrsservice.service;

import org.babynexign.babybilling.hrsservice.dto.*;
import org.babynexign.babybilling.hrsservice.entity.OperatorService;
import org.babynexign.babybilling.hrsservice.entity.Tariff;
import org.babynexign.babybilling.hrsservice.repository.TariffRepository;
import org.babynexign.babybilling.hrsservice.senders.BrtSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class TariffService {
    private final TariffRepository tariffRepository;
    private final BrtSender brtSender;

    @Autowired
    public TariffService(TariffRepository tariffRepository, BrtSender brtSender) {
        this.tariffRepository = tariffRepository;
        this.brtSender = brtSender;
    }

    @Transactional
    public void processTariffInformationRequest(TariffInformationRequest tariffInformationRequest) {
        Tariff tariff = tariffRepository.findById(tariffInformationRequest.tariffId())
                .orElseThrow(() -> new NoSuchElementException("Tariff not found"));

        List<QuantServiceDTO> quantServices = new ArrayList<>();
        List<ExtraServiceDTO> extraServices = new ArrayList<>();

        for (OperatorService service : tariff.getServices()) {
            if (service.getIsQuantitative() != null && service.getIsQuantitative()) {
                quantServices.add(new QuantServiceDTO(
                        service.getServiceType().getName().name(),
                        service.getAmount()
                ));
            } else {
                extraServices.add(new ExtraServiceDTO(
                        service.getId(),
                        service.getStartDate()
                ));
            }
        }

        brtSender.sendTariffInformationResponse(new TariffInformationResponse(
                tariffInformationRequest.personId(),
                tariffInformationRequest.tariffId(),
                quantServices,
                extraServices
        ));
    }

    public List<TariffDTO> getAllTariffs(){
        return tariffRepository.findAll().stream().map(TariffDTO::fromEntity).toList();
    }

    public TariffDTO getOneTariff(Long id){
        return TariffDTO.fromEntity(tariffRepository.findById(id).orElseThrow());
    }

    public TariffDTO createTariff(CreateTariffRequest createTariffRequest) {
        // TODO: implement
        return new TariffDTO(null, null, null, null, null, null, null, null, null, null);
    }
}
