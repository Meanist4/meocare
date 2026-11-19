package com.meocare.meocare.service;

import com.meocare.meocare.dto.LoginRequest;
import com.meocare.meocare.dto.LoginResponse;
import com.meocare.meocare.dto.RegisterRequest;
import com.meocare.meocare.dto.UserResponse;
import com.meocare.meocare.entity.Role;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.RoleRepository;
import com.meocare.meocare.repository.UserRepository;
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

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email đã được sử dụng");
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseThrow(() -> new IllegalStateException("Role CUSTOMER chưa được khởi tạo trong bảng roles"));

        User user = new User();
        user.setRole(customerRole);
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setPhone(request.phone());
        user.setStatus(UserStatus.ACTIVE);

        return UserResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        // Email không tồn tại và sai mật khẩu dùng chung một lỗi để không lộ email nào đã đăng ký.
        User user = userRepository.findWithRoleByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new BadCredentialsException("Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Tài khoản không ở trạng thái hoạt động");
        }

        return new LoginResponse("Đăng nhập thành công (JWT chưa được tích hợp)", null, UserResponse.from(user));
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findWithRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", id));
        return UserResponse.from(user);
    }

    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findWithRoleByEmail(normalizeEmail(email))
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng không tồn tại với email = " + email));
        return UserResponse.from(user);
    }

    /** Map sang DTO ngay trong transaction nên đọc role (LAZY) an toàn. */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserResponse::from).toList();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
