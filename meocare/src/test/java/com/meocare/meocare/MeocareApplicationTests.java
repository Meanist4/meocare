package com.meocare.meocare;

import static org.assertj.core.api.Assertions.assertThat;

import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.repository.RoleRepository;
import com.meocare.meocare.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class MeocareApplicationTests {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoadsAndSeedsRolesAndAdmin() {
        assertThat(roleRepository.existsByName(RoleName.ADMIN)).isTrue();
        assertThat(roleRepository.existsByName(RoleName.VETERINARIAN)).isTrue();
        assertThat(roleRepository.existsByName(RoleName.CUSTOMER)).isTrue();
        assertThat(userRepository.existsByRole_Name(RoleName.ADMIN)).isTrue();
    }
}
