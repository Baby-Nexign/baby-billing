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

    private Long msisdn;

    @Column(name = "money")
    private Long balance;

    @Column(name = "reg_data")
    private LocalDateTime registrationDate;

    private String description;

    @OneToMany(mappedBy = "subscriber", cascade = CascadeType.ALL)
    private List<CDRRecord> records;

    @OneToMany
    @JoinColumn(name = "p_id", referencedColumnName = "id")
    private List<QuantService> quantServices;

    @OneToMany
    @JoinColumn(name = "p_id", referencedColumnName = "id")
    private List<Tariff> tariffs;

    @OneToMany
    @JoinColumn(name = "p_id", referencedColumnName = "id")
    private List<ExtraService> extraServices;
}
