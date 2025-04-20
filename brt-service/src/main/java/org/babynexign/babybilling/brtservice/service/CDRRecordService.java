package org.babynexign.babybilling.brtservice.service;

import org.babynexign.babybilling.brtservice.dto.CallDTO;
import org.babynexign.babybilling.brtservice.entity.CDRRecord;
import org.babynexign.babybilling.brtservice.entity.Person;
import org.babynexign.babybilling.brtservice.entity.enums.RecordType;
import org.babynexign.babybilling.brtservice.repository.CDRRecordRepository;
import org.babynexign.babybilling.brtservice.repository.PersonRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CDRRecordService {
    private final CDRRecordRepository cdrRecordRepository;
    private final PersonRepository personRepository;

    @Autowired
    public CDRRecordService(CDRRecordRepository cdrRecordRepository, PersonRepository personRepository) {
        this.cdrRecordRepository = cdrRecordRepository;
        this.personRepository = personRepository;
    }

    public void processCDRs(List<CallDTO> callDTOs) {
        if (callDTOs == null || callDTOs.isEmpty()) {
            return;
        }

        for (CallDTO callDTO : callDTOs) {
            saveCallRecord(callDTO);
        }
    }

    public void saveCallRecord(CallDTO callDTO) {
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
            return;
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

        cdrRecordRepository.save(cdrRecord);
    }
}
