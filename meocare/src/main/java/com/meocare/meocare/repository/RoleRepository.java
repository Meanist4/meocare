package com.meocare.meocare.repository;

import com.meocare.meocare.entity.Role;
import com.meocare.meocare.entity.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
