package com.ilynkin.coding_assignment.repository;

import com.ilynkin.coding_assignment.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    Optional<UserRole> findByCode(String code);
}
