package org.babynexign.babybilling.hrsservice.repository;

import org.babynexign.babybilling.hrsservice.entity.TelecomType;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelecomTypeRepository extends JpaRepository<TelecomType, Long> {
    Optional<TelecomType> findByName(TelecomTypeName name);
}
