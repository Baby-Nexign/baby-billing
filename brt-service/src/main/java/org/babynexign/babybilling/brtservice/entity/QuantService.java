package org.babynexign.babybilling.brtservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.babynexign.babybilling.brtservice.entity.enums.QuantServiceType;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "quant_services")
public class QuantService {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "s_type_id")
    @Enumerated(EnumType.ORDINAL)
    private QuantServiceType serviceType;

    @Column(name = "amount_left")
    private Long amountLeft;
}
