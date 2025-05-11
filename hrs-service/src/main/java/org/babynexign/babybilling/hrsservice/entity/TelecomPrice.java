package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "telecom_prices")
public class TelecomPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Telecom type must be specified")
    @ManyToOne
    private TelecomType telecomType;

    @NotNull(message = "inOneNetwork must not be empty")
    @Column(name = "in_our_network")
    private Boolean inOurNetwork;

    @NotNull(message = "Telecom data type must be specified")
    @ManyToOne
    private TelecomDataType telecomDataType;

    @Min(value = 0, message = "Cost cannot be negative")
    @Column(name = "cost")
    private Long cost;
}
