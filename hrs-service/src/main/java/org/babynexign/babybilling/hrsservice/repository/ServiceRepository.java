package org.babynexign.babybilling.hrsservice.repository;

import org.babynexign.babybilling.hrsservice.entity.OperatorService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends JpaRepository<OperatorService, Long> {
}
