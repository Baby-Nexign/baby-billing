package org.babynexign.babybilling.hrsservice.repository;

import org.babynexign.babybilling.hrsservice.entity.TelecomDataType;
import org.babynexign.babybilling.hrsservice.entity.enums.TelecomDataTypeName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelecomDataTypeRepository extends JpaRepository<TelecomDataType, Long> {
    Optional<TelecomDataType> findByName(TelecomDataTypeName name);
}
