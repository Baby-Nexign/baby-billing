package org.babynexign.babybilling.brtservice.service;

import org.babynexign.babybilling.brtservice.dto.billing.BillingRequest;
import org.babynexign.babybilling.brtservice.dto.CallDTO;
import org.babynexign.babybilling.brtservice.entity.CDRRecord;
import org.babynexign.babybilling.brtservice.entity.Person;
import org.babynexign.babybilling.brtservice.entity.QuantService;
import org.babynexign.babybilling.brtservice.entity.Tariff;
import org.babynexign.babybilling.brtservice.entity.enums.QuantServiceType;
import org.babynexign.babybilling.brtservice.entity.enums.RecordType;
import org.babynexign.babybilling.brtservice.exception.InvalidCallDateException;
import org.babynexign.babybilling.brtservice.exception.InvalidCallTypeException;
import org.babynexign.babybilling.brtservice.repository.CDRRecordRepository;
import org.babynexign.babybilling.brtservice.repository.PersonRepository;
import org.babynexign.babybilling.brtservice.senders.HrsSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CDRRecordService {
    private final CDRRecordRepository cdrRecordRepository;
    private final PersonRepository personRepository;
    private final HrsSender hrsSender;

    @Autowired
    public CDRRecordService(CDRRecordRepository cdrRecordRepository, PersonRepository personRepository, HrsSender hrsSender) {
        this.cdrRecordRepository = cdrRecordRepository;
        this.personRepository = personRepository;
        this.hrsSender = hrsSender;
    }

    @Transactional
    public void processCDRs(List<CallDTO> callDTOs) {
        if (callDTOs == null || callDTOs.isEmpty()) {
            return;
        }

        for (CallDTO callDTO : callDTOs) {
            try {
                CDRRecord record = saveCallRecord(callDTO);
                if (record != null) {
                    processBilling(record);
                }
            } catch (Exception e) {
                System.err.println("Error processing CDR: " + e.getMessage() +
                                 " (First MSISDN: " + callDTO.firstSubscriberMsisdn() + 
                                 ", Second MSISDN: " + callDTO.secondSubscriberMsisdn() + ")");
            }
        }
    }

    private CDRRecord saveCallRecord(CallDTO callDTO) {
        try {
            RecordType recordType;
            try {
                recordType = switch (callDTO.callType()) {
                    case "01" -> RecordType.OUTCOMING;
                    case "02" -> RecordType.INCOMING;
                    default -> throw new IllegalArgumentException("Unknown call type: " + callDTO.callType());
                };
            } catch (IllegalArgumentException e) {
                throw new InvalidCallTypeException("Invalid call type: " + callDTO.callType());
            }

            Optional<Person> firstPerson = personRepository.findByMsisdn(callDTO.firstSubscriberMsisdn());
            Optional<Person> secondPerson = personRepository.findByMsisdn(callDTO.secondSubscriberMsisdn());
            Boolean inOneNetwork = firstPerson.isPresent() && secondPerson.isPresent();
            Person subscriber = firstPerson.orElse(null);

            if (subscriber == null) {
                return null;
            }

            if (callDTO.callStart() == null || callDTO.callEnd() == null) {
                throw new InvalidCallDateException("Call start and end times must be provided. First MSISDN: " + 
                    callDTO.firstSubscriberMsisdn() + ", Second MSISDN: " + callDTO.secondSubscriberMsisdn());
            }

            if (callDTO.callEnd().isBefore(callDTO.callStart())) {
                throw new InvalidCallDateException("Call end time cannot be before call start time. First MSISDN: " + 
                    callDTO.firstSubscriberMsisdn() + ", Second MSISDN: " + callDTO.secondSubscriberMsisdn());
            }

            Duration callDuration = Duration.between(callDTO.callStart(), callDTO.callEnd());

            CDRRecord cdrRecord = CDRRecord.builder()
                    .type(recordType)
                    .firstMsisdn(callDTO.firstSubscriberMsisdn())
                    .secondMsisdn(callDTO.secondSubscriberMsisdn())
                    .callStart(callDTO.callStart())
                    .callDuration(callDuration)
                    .inOneNetwork(inOneNetwork)
                    .subscriber(subscriber)
                    .build();

            return cdrRecordRepository.save(cdrRecord);
        } catch (Exception e) {
            if (e instanceof InvalidCallDateException || e instanceof InvalidCallTypeException) {
                throw e;
            }
            throw new RuntimeException("Error processing CDR record: " + e.getMessage(), e);
        }
    }

    private void processBilling(CDRRecord record) {
        Optional<QuantService> quantServiceOpt = record.getSubscriber().getQuantServices().stream()
                .filter(service -> service.getServiceType() == QuantServiceType.MINUTES)
                .findFirst();

        Tariff tariff = record.getSubscriber().getTariff();

        if (quantServiceOpt.isPresent()) {
            QuantService quantService = quantServiceOpt.get();

            if (quantService.getAmountLeft() >= record.getDurationInMinutes()) {
                quantService.setAmountLeft(quantService.getAmountLeft() - record.getDurationInMinutes());
            } else {
                Long minutesToBill = record.getDurationInMinutes() - quantService.getAmountLeft();
                quantService.setAmountLeft(0L);
                BillingRequest billingRequest = new BillingRequest(
                        record.getSubscriber().getId(),
                        tariff.getTariffId(),
                        minutesToBill,
                        record.getType().toString(),
                        record.getInOneNetwork()
                );
                hrsSender.sendBillingRequest(billingRequest);
            }
        } else {
            BillingRequest billingRequest = new BillingRequest(
                    record.getSubscriber().getId(),
                    tariff.getTariffId(),
                    record.getDurationInMinutes(),
                    record.getType().toString(),
                    record.getInOneNetwork()
            );
            hrsSender.sendBillingRequest(billingRequest);
        }
    }
}
