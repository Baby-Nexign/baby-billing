package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotBlank(message = "Service name should be specified")
    @Column(name = "name", unique = true)
    private String name;

    @NotNull(message = "Service type must be specified")
    @ManyToOne
    private ServiceType serviceType;

    @NotNull(message = "isQuantitative must not be empty")
    @Column(name = "is_quantitative")
    private Boolean isQuantitative;

    @NotNull(message = "Start date must not be empty")
    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "av_date")
    private LocalDate avDate;

    @Column(name = "ac_end_date")
    private LocalDate acEndDate;

    @Min(value = 0, message = "Amount cannot be negative")
    @Column(name = "amount")
    private Long amount;

    @Column(name = "description")
    private String description;

    @Min(value = 0, message = "Cost cannot be negative")
    @Column(name = "cost")
    private Long cost;

    @Min(value = 0, message = "Period cannot be negative")
    @Column(name = "period")
    private Integer period;
}
