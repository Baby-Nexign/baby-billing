package org.babynexign.babybilling.brtservice.dto;

import java.time.LocalDateTime;

public record CallDTO(
        String callType,
        String firstSubscriberMsisdn,
        String  secondSubscriberMsisdn,
        LocalDateTime callStart,
        LocalDateTime callEnd
) {
}
