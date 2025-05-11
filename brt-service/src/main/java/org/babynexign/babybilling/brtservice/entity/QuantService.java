package org.babynexign.babybilling.brtservice.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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

    @NotNull(message = "Service type must be specified")
    @Column(name = "s_type_id")
    @Enumerated(EnumType.ORDINAL)
    private QuantServiceType serviceType;

    @Min(value = 0, message = "Amount left cannot be negative")
    @Column(name = "amount_left")
    private Long amountLeft;
}
