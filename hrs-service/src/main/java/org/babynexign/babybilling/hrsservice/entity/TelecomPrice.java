package org.babynexign.babybilling.hrsservice.entity;

import jakarta.persistence.*;
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

    @ManyToOne
    private TelecomType telecomType;

    @Column(name = "in_our_network")
    private Boolean inOurNetwork;

    @ManyToOne
    private TelecomDataType telecomDataType;

    @Column(name = "cost")
    private Long cost;
}
