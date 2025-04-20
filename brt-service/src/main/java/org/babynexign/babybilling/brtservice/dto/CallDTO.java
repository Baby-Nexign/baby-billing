package org.babynexign.babybilling.brtservice.dto;

import java.time.LocalDateTime;

public record CallDTO(
        Long id,
        String callType,
        Long firstSubscriberMsisdn,
        Long secondSubscriberMsisdn,
        LocalDateTime callStart,
        LocalDateTime callEnd
) {}
