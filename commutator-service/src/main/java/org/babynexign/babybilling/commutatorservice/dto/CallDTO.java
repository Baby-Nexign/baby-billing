package org.babynexign.babybilling.commutatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.babynexign.babybilling.commutatorservice.entity.Call;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CallDTO implements Serializable {
    private Long id;
    private String callType;
    private Long callingSubscriberMsisdn;
    private Long receivingSubscriberMsisdn;
    private LocalDateTime callStart;
    private LocalDateTime callEnd;

    public static CallDTO fromEntity(Call call) {
        if (call == null) {
            return null;
        }

        return CallDTO.builder()
                .id(call.getId())
                .callType(call.getCallType().getIndex())
                .callingSubscriberMsisdn(call.getCallingSubscriber().getMsisdn())
                .receivingSubscriberMsisdn(call.getReceivingSubscriber().getMsisdn())
                .callStart(call.getCallStart())
                .callEnd(call.getCallEnd())
                .build();
    }
}
