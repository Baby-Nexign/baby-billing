package org.babynexign.babybilling.commutatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.babynexign.babybilling.commutatorservice.entity.Record;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecordDTO implements Serializable {
    private Long id;
    private String callType;
    private Long callingSubscriberMsisdn;
    private Long receivingSubscriberMsisdn;
    private LocalDateTime callStart;
    private LocalDateTime callEnd;

    public static RecordDTO fromEntity(Record record) {
        if (record == null) {
            return null;
        }

        return RecordDTO.builder()
                .id(record.getId())
                .callType(record.getCallType().getIndex())
                .callingSubscriberMsisdn(record.getCallingSubscriber().getMsisdn())
                .receivingSubscriberMsisdn(record.getReceivingSubscriber().getMsisdn())
                .callStart(record.getCallStart())
                .callEnd(record.getCallEnd())
                .build();
    }
}
