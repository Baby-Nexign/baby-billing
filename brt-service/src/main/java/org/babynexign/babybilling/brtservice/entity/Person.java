package org.babynexign.babybilling.brtservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Person {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String msisdn;

    @Column(name = "money")
    private Long balance;

    private Boolean isRestricted;

    @Column(name = "reg_data")
    private LocalDateTime registrationDate;

    private String description;

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL)
    private List<CDRRecord> records;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "p_id", referencedColumnName = "id")
    private List<QuantService> quantServices;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "tariff_id", referencedColumnName = "id")
    private Tariff tariff;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "p_id", referencedColumnName = "id")
    private List<ExtraService> extraServices;
}
