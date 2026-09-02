package org.ecommerce.project.repository;

import org.ecommerce.project.model.AppRole;
import org.ecommerce.project.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long> {
    Optional<Role> findByRoleName(AppRole roleName);
}
