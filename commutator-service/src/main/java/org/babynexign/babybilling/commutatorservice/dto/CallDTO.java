package org.babynexign.babybilling.commutatorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.babynexign.babybilling.commutatorservice.entity.Call;

import java.io.Serializable;
import java.time.LocalDateTime;

public record CallDTO(
        Long id,
        String callType,
        Long callingSubscriberMsisdn,
        Long receivingSubscriberMsisdn,
        LocalDateTime callStart,
        LocalDateTime callEnd
) implements Serializable {
    public static CallDTO fromEntity(Call call) {
        if (call == null) {
            return null;
        }
        return new CallDTO(
                call.getId(),
                call.getCallType().getIndex(),
                call.getCallingSubscriber().getMsisdn(),
                call.getReceivingSubscriber().getMsisdn(),
                call.getCallStart(),
                call.getCallEnd()
        );
    }
}
