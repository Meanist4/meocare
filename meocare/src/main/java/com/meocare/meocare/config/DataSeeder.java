package com.meocare.meocare.config;

import com.meocare.meocare.entity.Role;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import com.meocare.meocare.repository.RoleRepository;
import com.meocare.meocare.repository.UserRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Idempotently seeds required clinic roles (ADMIN, VETERINARIAN, CUSTOMER)
 * and creates a configurable initial ADMIN account if none exists yet.
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final Map<RoleName, String> ROLE_DESCRIPTIONS = Map.of(
            RoleName.ADMIN, "Clinic administrator with full operational and staff management access",
            RoleName.VETERINARIAN, "Licensed clinic veterinarian managing examinations, medical records, and health updates",
            RoleName.CUSTOMER, "Pet parent managing pet profiles, appointment requests, and health timelines"
    );

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.full-name:Clinic Administrator}")
    private String adminFullName;

    @Value("${app.bootstrap.admin.email:admin@meocare.clinic}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:Admin@123456}")
    private String adminPassword;

    @Value("${app.bootstrap.admin.phone:+1 (800) 555-0100}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedInitialAdmin();
    }

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                Role role = new Role();
                role.setName(roleName);
                role.setDescription(ROLE_DESCRIPTIONS.getOrDefault(roleName, roleName.name()));
                roleRepository.save(role);
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    private void seedInitialAdmin() {
        if (userRepository.existsByRole_Name(RoleName.ADMIN)) {
            return;
        }
        String normalizedEmail = adminEmail.trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            log.warn("Bootstrap admin email {} already exists under a non-ADMIN role; skipping admin bootstrap.", normalizedEmail);
            return;
        }

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role must exist before seeding admin user"));

        User admin = new User();
        admin.setRole(adminRole);
        admin.setFullName(adminFullName.trim());
        admin.setEmail(normalizedEmail);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setPhone(adminPhone != null ? adminPhone.trim() : null);
        admin.setStatus(UserStatus.ACTIVE);
        userRepository.save(admin);

        log.info("Seeded bootstrap ADMIN account: {}", normalizedEmail);
    }
}
