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

    /** Dùng cho login/profile: nạp luôn role để tránh LazyInitializationException. */
    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleByEmail(String email);

    @EntityGraph(attributePaths = "role")
    Optional<User> findWithRoleById(Long id);

    /** Ví dụ: danh sách bác sĩ đang hoạt động. */
    List<User> findByRole_NameAndStatus(RoleName roleName, UserStatus status);
}
