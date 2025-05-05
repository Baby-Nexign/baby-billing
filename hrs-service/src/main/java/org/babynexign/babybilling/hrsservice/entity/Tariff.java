package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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

    @OneToOne
    private TariffType type;

    @Column(name = "cost")
    private Long cost;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "av_end_date")
    private LocalDateTime avEndDate;

    @Column(name = "ac_end_date")
    private LocalDateTime acEndDate;

    @Column(name = "description")
    private String description;

    @ManyToMany
    private List<OperatorService> services;

    @OneToMany
    private List<TelecomPrice> telecomPrices;
}
