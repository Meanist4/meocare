package com.meocare.meocare.service;

import com.meocare.meocare.dto.CreateStaffUserRequest;
import com.meocare.meocare.dto.LoginRequest;
import com.meocare.meocare.dto.LoginResponse;
import com.meocare.meocare.dto.RegisterRequest;
import com.meocare.meocare.dto.UpdateProfileRequest;
import com.meocare.meocare.dto.UserResponse;
import com.meocare.meocare.entity.Role;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.RoleRepository;
import com.meocare.meocare.repository.UserRepository;
import com.meocare.meocare.security.JwtService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER is not initialized in roles table"));

        User user = new User();
        user.setRole(customerRole);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(trimToNull(request.phone()));
        user.setStatus(UserStatus.ACTIVE);

        return UserResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        // Use a single error message for unknown email and wrong password to avoid account enumeration.
        User user = userRepository.findWithRoleByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Account is not active");
        }

        String token = jwtService.generateToken(user);
        return new LoginResponse("Login successful", token, UserResponse.from(user));
    }

    public UserResponse getProfile(Long userId) {
        return getUserById(userId);
    }

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findWithRoleById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id = " + userId));

        if (request.fullName() != null) {
            String trimmedName = request.fullName().trim();
            if (trimmedName.isEmpty()) {
                throw new BadRequestException("Full name cannot be blank");
            }
            user.setFullName(trimmedName);
        }

        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }

        if (request.avatarUrl() != null) {
            user.setAvatarUrl(trimToNull(request.avatarUrl()));
        }

        if (request.newPassword() != null && !request.newPassword().isEmpty()) {
            if (request.currentPassword() == null
                    || !passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new BadRequestException("Current password is incorrect");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse createStaffUser(CreateStaffUserRequest request) {
        if (request.role() != RoleName.VETERINARIAN && request.role() != RoleName.ADMIN) {
            throw new BadRequestException("Admin staff provisioning only supports VETERINARIAN or ADMIN roles");
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email is already in use");
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new IllegalStateException("Role " + request.role() + " is not initialized"));

        User user = new User();
        user.setRole(role);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(trimToNull(request.phone()));
        user.setAvatarUrl(trimToNull(request.avatarUrl()));
        user.setStatus(UserStatus.ACTIVE);

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUserStatus(Long targetUserId, UserStatus newStatus, Long actorUserId) {
        if (actorUserId != null && actorUserId.equals(targetUserId) && newStatus != UserStatus.ACTIVE) {
            throw new BadRequestException("Administrators cannot deactivate or lock their own account");
        }

        User user = userRepository.findWithRoleById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id = " + targetUserId));
        user.setStatus(newStatus);

        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findWithRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id = " + id));
        return UserResponse.from(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findWithRoleByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email = " + email));
        return UserResponse.from(user);
    }

    public List<UserResponse> getAllUsers() {
        return getUsers(null, null);
    }

    public List<UserResponse> getUsers(RoleName role, UserStatus status) {
        List<User> users;
        if (role != null && status != null) {
            users = userRepository.findByRole_NameAndStatusOrderByCreatedAtDesc(role, status);
        } else if (role != null) {
            users = userRepository.findByRole_NameOrderByCreatedAtDesc(role);
        } else if (status != null) {
            users = userRepository.findByStatusOrderByCreatedAtDesc(status);
        } else {
            users = userRepository.findAllByOrderByCreatedAtDesc();
        }
        return users.stream().map(UserResponse::from).toList();
    }

    public List<UserResponse> getActiveVeterinarians() {
        return userRepository.findByRole_NameAndStatusOrderByCreatedAtDesc(RoleName.VETERINARIAN, UserStatus.ACTIVE)
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
