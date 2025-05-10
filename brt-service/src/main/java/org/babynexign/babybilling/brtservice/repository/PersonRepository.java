package org.babynexign.babybilling.brtservice.repository;

import org.babynexign.babybilling.brtservice.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByMsisdn(String msisdn);

    List<Person> findAllByIsRestrictedFalse();
}
