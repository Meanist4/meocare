package com.meocare.meocare.repository;

import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole_Name(RoleName roleName);

    /** Eagerly fetch role for login/profile to avoid LazyInitializationException. */
    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(Long id);

    @EntityGraph(attributePaths = "role")
    List<User> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "role")
    List<User> findByRole_NameOrderByCreatedAtDesc(RoleName roleName);

    @EntityGraph(attributePaths = "role")
    List<User> findByStatusOrderByCreatedAtDesc(UserStatus status);

    @EntityGraph(attributePaths = "role")
    List<User> findByRole_NameAndStatusOrderByCreatedAtDesc(RoleName roleName, UserStatus status);

    /** Active users by role (e.g., active veterinarians). */
    @EntityGraph(attributePaths = "role")
    List<User> findByRole_NameAndStatus(RoleName roleName, UserStatus status);
}
