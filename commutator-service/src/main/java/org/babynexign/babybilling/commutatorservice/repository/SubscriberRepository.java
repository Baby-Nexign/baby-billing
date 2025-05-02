package org.babynexign.babybilling.commutatorservice.repository;

import org.babynexign.babybilling.commutatorservice.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    Optional<Subscriber> findByMsisdn(String msisdn);

    List<Subscriber> findByIsRestrictedFalse();
}
