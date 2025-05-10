package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tariffs")
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "payment_period")
    private Integer paymentPeriod;

    @Column(name = "cost")
    private Long cost;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "av_end_date")
    private LocalDate avEndDate;

    @Column(name = "ac_end_date")
    private LocalDate acEndDate;

    @Column(name = "description")
    private String description;

    @ManyToMany
    private List<OperatorService> services;

    @OneToMany
    private List<TelecomPrice> telecomPrices;
}
