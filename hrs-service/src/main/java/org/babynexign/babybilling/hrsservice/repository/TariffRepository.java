package org.babynexign.babybilling.hrsservice.repository;

import org.babynexign.babybilling.hrsservice.entity.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {
}
