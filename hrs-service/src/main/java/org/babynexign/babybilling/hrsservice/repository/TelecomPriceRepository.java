package org.babynexign.babybilling.hrsservice.repository;

import org.babynexign.babybilling.hrsservice.entity.TelecomPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelecomPriceRepository extends JpaRepository<TelecomPrice, Long> {
}
