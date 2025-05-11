package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotBlank(message = "Tariff name must not be empty")
    @Column(name = "name", unique = true)
    private String name;

    @NotNull(message = "Payment period must be specified")
    @Column(name = "payment_period")
    private Integer paymentPeriod;

    @Min(value = 0, message = "Cost cannot be negative")
    @Column(name = "cost")
    private Long cost;

    @NotNull(message = "Start date must be specified")
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
