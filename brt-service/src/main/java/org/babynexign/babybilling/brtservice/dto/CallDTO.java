package org.babynexign.babybilling.brtservice.dto;

import java.time.LocalDateTime;

public record CallDTO(
        Long id,
        String callType,
        Long callingSubscriberMsisdn,
        Long receivingSubscriberMsisdn,
        LocalDateTime callStart,
        LocalDateTime callEnd
) {}
