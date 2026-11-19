package com.meocare.meocare.support;

import com.meocare.meocare.entity.Role;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static Role defaultRole(RoleName roleName) {
        Role role = new Role();
        role.setName(roleName);
        return role;
    }

    public static User sampleUser(Long id, Role role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setFullName("Test User");
        user.setEmail("test@meocare.local");
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
