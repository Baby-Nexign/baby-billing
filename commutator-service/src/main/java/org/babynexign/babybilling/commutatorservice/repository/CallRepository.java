package org.babynexign.babybilling.commutatorservice.repository;

import org.babynexign.babybilling.commutatorservice.entity.Call;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CallRepository extends JpaRepository<Call, Long> {
    /**
     * Retrieves all call records sorted by call start time in ascending order.
     *
     * @param pageable pagination information
     * @return a page of call records ordered by their start time (oldest first)
     */
    Page<Call> findAllByOrderByCallStartAsc(Pageable pageable);
}
