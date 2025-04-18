package org.babynexign.babybilling.commutatorservice.repository;

import org.babynexign.babybilling.commutatorservice.entity.Subscriber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {}
