package org.babynexign.babybilling.hrsservice.service;

import org.babynexign.babybilling.hrsservice.dto.*;
import org.babynexign.babybilling.hrsservice.entity.*;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomDataTypeName;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomTypeName;
import org.babynexign.babybilling.hrsservice.exception.ServiceNotFoundException;
import org.babynexign.babybilling.hrsservice.exception.TariffNotFoundException;
import org.babynexign.babybilling.hrsservice.exception.TariffValidationException;
import org.babynexign.babybilling.hrsservice.exception.TelecomPriceNotFoundException;
import org.babynexign.babybilling.hrsservice.repository.*;
import org.babynexign.babybilling.hrsservice.senders.BrtSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class TariffService {
    private final TariffRepository tariffRepository;
    private final ServiceRepository serviceRepository;
    private final TelecomDataTypeRepository telecomDataTypeRepository;
    private final TelecomTypeRepository telecomTypeRepository;
    private final TelecomPriceRepository telecomPriceRepository;
    private final BrtSender brtSender;

    @Autowired
    public TariffService(TariffRepository tariffRepository, ServiceRepository serviceRepository, TelecomDataTypeRepository telecomDataTypeRepository, TelecomTypeRepository telecomTypeRepository, TelecomPriceRepository telecomPriceRepository, BrtSender brtSender) {
        this.tariffRepository = tariffRepository;
        this.serviceRepository = serviceRepository;
        this.telecomDataTypeRepository = telecomDataTypeRepository;
        this.telecomTypeRepository = telecomTypeRepository;
        this.telecomPriceRepository = telecomPriceRepository;
        this.brtSender = brtSender;
    }

    @Transactional
    public void processTariffInformationRequest(TariffInformationRequest tariffInformationRequest) {
        if (tariffInformationRequest.tariffId() == null || tariffInformationRequest.personId() == null) {
            throw new TariffValidationException("Tariff ID and Person ID cannot be null");
        }
        
        Tariff tariff = tariffRepository.findById(tariffInformationRequest.tariffId())
                .orElseThrow(() -> new TariffNotFoundException("Tariff with ID " + tariffInformationRequest.tariffId() + " not found"));

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

    public void processCountTariffPaymentRequest(CountTariffPaymentRequest countTariffPaymentRequest){
        if (countTariffPaymentRequest.tariffId() == null || 
            countTariffPaymentRequest.personId() == null ||
            countTariffPaymentRequest.startDate() == null ||
            countTariffPaymentRequest.currentDate() == null) {
            throw new TariffValidationException("Tariff ID, Person ID, Start date and Current date cannot be null");
        }
        
        if (countTariffPaymentRequest.currentDate().isBefore(countTariffPaymentRequest.startDate())) {
            throw new TariffValidationException("Current date cannot be before start date");
        }
        
        Tariff tariff = tariffRepository.findById(countTariffPaymentRequest.tariffId())
                .orElseThrow(() -> new TariffNotFoundException("Tariff with ID " + countTariffPaymentRequest.tariffId() + " not found"));

        long daysBetween = ChronoUnit.DAYS.between(
                countTariffPaymentRequest.startDate(),
                countTariffPaymentRequest.currentDate());

        if (daysBetween % tariff.getPaymentPeriod() == 0){
           brtSender.sendCountTariffPaymentResponse(new CountTariffPaymentResponse(countTariffPaymentRequest.personId(), tariff.getCost()));
        }
    }

    public List<TariffDTO> getAllTariffs(){
        return tariffRepository.findAll().stream().map(TariffDTO::fromEntity).toList();
    }

    public TariffDTO getOneTariff(Long id){
        return TariffDTO.fromEntity(tariffRepository.findById(id)
                .orElseThrow(() -> new TariffNotFoundException("Tariff with ID " + id + " not found")));
    }

    @Transactional
    public TariffDTO createTariff(CreateTariffRequest createTariffRequest) {
        Tariff tariff = Tariff.builder()
                .name(createTariffRequest.name())
                .paymentPeriod(createTariffRequest.paymentPeriod())
                .cost(createTariffRequest.cost())
                .description(createTariffRequest.description())
                .startDate(LocalDate.now())
                .services(new ArrayList<>())
                .telecomPrices(new ArrayList<>())
                .build();

        if (createTariffRequest.serviceIds() != null && !createTariffRequest.serviceIds().isEmpty()) {
            List<OperatorService> services = serviceRepository.findAllById(createTariffRequest.serviceIds());
            if (services.size() != createTariffRequest.serviceIds().size()) {
                throw new ServiceNotFoundException("One or more services with requested IDs not found");
            }
            tariff.setServices(services);
        }

        CallPricesDTO callPrices = createTariffRequest.callPrices();
        if (callPrices != null) {
            List<TelecomPrice> telecomPrices = new ArrayList<>();

            TelecomType incomingType = telecomTypeRepository.findByName(TelecomTypeName.INCOMING)
                    .orElseThrow(() -> new TelecomPriceNotFoundException("INCOMING TelecomType not found"));
            TelecomType outgoingType = telecomTypeRepository.findByName(TelecomTypeName.OUTCOMING)
                    .orElseThrow(() -> new TelecomPriceNotFoundException("OUTCOMING TelecomType not found"));

            TelecomDataType minutesType = telecomDataTypeRepository.findByName(TelecomDataTypeName.MINUTES)
                    .orElseThrow(() -> new TelecomPriceNotFoundException("MINUTES TelecomDataType not found"));

            telecomPrices.add(TelecomPrice.builder()
                    .telecomType(incomingType)
                    .inOurNetwork(true)
                    .telecomDataType(minutesType)
                    .cost(callPrices.incomingInNetworkPrice())
                    .build());

            telecomPrices.add(TelecomPrice.builder()
                    .telecomType(outgoingType)
                    .inOurNetwork(true)
                    .telecomDataType(minutesType)
                    .cost(callPrices.outgoingInNetworkPrice())
                    .build());

            telecomPrices.add(TelecomPrice.builder()
                    .telecomType(incomingType)
                    .inOurNetwork(false)
                    .telecomDataType(minutesType)
                    .cost(callPrices.incomingOutNetworkPrice())
                    .build());

            telecomPrices.add(TelecomPrice.builder()
                    .telecomType(outgoingType)
                    .inOurNetwork(false)
                    .telecomDataType(minutesType)
                    .cost(callPrices.outgoingOutNetworkPrice())
                    .build());

            telecomPriceRepository.saveAll(telecomPrices);
            tariff.setTelecomPrices(telecomPrices);
        }

        Tariff savedTariff = tariffRepository.save(tariff);

        return TariffDTO.fromEntity(savedTariff);
    }
}
