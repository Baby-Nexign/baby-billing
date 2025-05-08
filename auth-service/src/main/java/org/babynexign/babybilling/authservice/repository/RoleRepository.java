package org.babynexign.babybilling.authservice.repository;

import org.babynexign.babybilling.authservice.entity.Role;
import org.babynexign.babybilling.authservice.entity.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(ERole name);
}
