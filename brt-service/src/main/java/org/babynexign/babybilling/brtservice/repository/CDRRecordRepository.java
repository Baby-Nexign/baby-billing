package org.babynexign.babybilling.brtservice.repository;

import org.babynexign.babybilling.brtservice.entity.CDRRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CDRRecordRepository extends JpaRepository<CDRRecord, Long> {
}
