package org.babynexign.babybilling.commutatorservice.repository;

import org.babynexign.babybilling.commutatorservice.entity.Call;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRepository extends JpaRepository<Call, Long> {
    Page<Call> findAllByOrderByCallStartAsc(Pageable pageable);
}
