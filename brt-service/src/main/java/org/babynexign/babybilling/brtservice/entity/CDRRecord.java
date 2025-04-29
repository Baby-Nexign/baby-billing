package org.babynexign.babybilling.brtservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.babynexign.babybilling.brtservice.converter.RecordTypeConverter;
import org.babynexign.babybilling.brtservice.entity.enums.RecordType;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "cdr_record")
public class CDRRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = RecordTypeConverter.class)
    private RecordType type;

    @Column(name = "msisdn_one")
    private String firstMsisdn;

    @Column(name = "msisdn_two")
    private String secondMsisdn;

    @Column(name = "start_time")
    private LocalDateTime callStart;

    @Column(name = "lasts")
    private Duration callDuration;

    @Column(name = "in_one_network")
    private Boolean inOneNetwork;

    @ManyToOne
    @JoinColumn(name = "our_subscriber_id", referencedColumnName = "id")
    private Person subscriber;

    public Long getDurationInMinutes() {
        long minutes = callDuration.toMinutes();

        if (callDuration.getSeconds() > 0 || callDuration.getNano() > 0) {
            minutes += 1;
        }

        return minutes;
    }
}
