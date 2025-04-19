package org.babynexign.babybilling.commutatorservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.babynexign.babybilling.commutatorservice.converter.CallTypeConverter;
import org.babynexign.babybilling.commutatorservice.entity.enums.CallType;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = CallTypeConverter.class)
    private CallType callType;

    @ManyToOne
    @JoinColumn(name = "calling_subscriber_msisdn")
    private Subscriber callingSubscriber;

    @ManyToOne
    @JoinColumn(name = "receiving_subscriber_msisdn")
    private Subscriber receivingSubscriber;

    private LocalDateTime callStart;
    private LocalDateTime callEnd;
}
