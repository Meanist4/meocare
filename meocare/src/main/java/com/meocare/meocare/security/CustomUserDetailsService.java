package com.meocare.meocare.security;

import com.meocare.meocare.entity.User;
import com.meocare.meocare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (email == null || email.isBlank()) {
            throw new UsernameNotFoundException("Email must not be blank");
        }
        String normalized = email.trim().toLowerCase();
        User user = userRepository.findWithRoleByEmail(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + normalized));
        return toUserDetails(user);
    }

    public CustomUserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        if (userId == null) {
            throw new UsernameNotFoundException("User id must not be null");
        }
        User user = userRepository.findWithRoleById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        return toUserDetails(user);
    }

    private CustomUserDetails toUserDetails(User user) {
        if (user.getRole() == null || user.getRole().getName() == null) {
            throw new UsernameNotFoundException("User has no assigned role: " + user.getEmail());
        }
        return CustomUserDetails.from(user);
    }
}
