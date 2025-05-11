package org.babynexign.babybilling.brtservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotNull(message = "Record type must be specified")
    @Convert(converter = RecordTypeConverter.class)
    private RecordType type;

    @Pattern(regexp = "^[0-9]{11}$", message = "MSISDN must contain exactly 11 digits")
    @Column(name = "msisdn_one")
    private String firstMsisdn;

    @Pattern(regexp = "^[0-9]{11}$", message = "MSISDN must contain exactly 11 digits")
    @Column(name = "msisdn_two")
    private String secondMsisdn;

    @NotNull(message = "Call start time must be specified")
    @Column(name = "start_time")
    private LocalDateTime callStart;

    @NotNull(message = "Call duration must be specified")
    @Column(name = "lasts")
    private Duration callDuration;

    @NotNull(message = "inOneNetwork must not be empty")
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
