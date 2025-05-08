package org.babynexign.babybilling.authservice.repository;

import org.babynexign.babybilling.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByMsisdn(String msisdn);
    Boolean existsByUsername(String username);
    Boolean existsByMsisdn(String msisdn);
}
