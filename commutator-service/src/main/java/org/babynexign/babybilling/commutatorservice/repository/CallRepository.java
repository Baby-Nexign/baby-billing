package org.babynexign.babybilling.commutatorservice.repository;

import org.babynexign.babybilling.commutatorservice.entity.Record;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRepository extends JpaRepository<Record, Long> {
    Page<Record> findAllByOrderByCallStartAsc(Pageable pageable);
}
