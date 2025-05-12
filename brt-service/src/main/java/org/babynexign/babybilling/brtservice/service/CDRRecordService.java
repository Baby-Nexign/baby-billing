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
            CDRRecord record = saveCallRecord(callDTO);
            if (record != null) {
                processBilling(record);
            }
        }
    }

    private CDRRecord saveCallRecord(CallDTO callDTO) {
        RecordType recordType;
        recordType = switch (callDTO.callType()) {
            case "01" -> RecordType.OUTCOMING;
            case "02" -> RecordType.INCOMING;
            default -> throw new IllegalArgumentException("Unknown call type: " + callDTO.callType());
        };

        Optional<Person> firstPerson = personRepository.findByMsisdn(callDTO.firstSubscriberMsisdn());
        Optional<Person> secondPerson = personRepository.findByMsisdn(callDTO.secondSubscriberMsisdn());
        Boolean inOneNetwork = firstPerson.isPresent() && secondPerson.isPresent();
        Person subscriber = firstPerson.orElse(null);

        if (subscriber == null) {
            return null;
        }

        if (callDTO.callStart() != null && callDTO.callEnd() != null && callDTO.callEnd().isBefore(callDTO.callStart())) {
            throw new InvalidCallDateException("Call end time cannot be before call start time. First MSISDN: " + 
                callDTO.firstSubscriberMsisdn() + ", Second MSISDN: " + callDTO.secondSubscriberMsisdn());
        }

        Duration callDuration = null;
        if (callDTO.callStart() != null && callDTO.callEnd() != null) {
            callDuration = Duration.between(callDTO.callStart(), callDTO.callEnd());
        }

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
    }

    private void processBilling(CDRRecord record) {
        QuantService quantService = record.getSubscriber().getQuantServices().stream()
                .filter(service -> service.getServiceType() == QuantServiceType.MINUTES)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("QuantService with serviceTypeId=1 not found"));

        Tariff tariff = record.getSubscriber().getTariff();

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
    }
}
