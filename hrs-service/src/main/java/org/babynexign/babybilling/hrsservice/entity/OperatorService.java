package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "services")
public class OperatorService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @ManyToOne
    private ServiceType serviceType;

    @Column(name = "is_quantitative")
    private Boolean isQuantitative;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "av_date")
    private LocalDate avDate;

    @Column(name = "ac_end_date")
    private LocalDate acEndDate;

    @Column(name = "amount")
    private Long amount;

    @Column(name = "description")
    private String description;

    @Column(name = "cost")
    private Long cost;

    @Column(name = "period")
    private Integer period;
}
