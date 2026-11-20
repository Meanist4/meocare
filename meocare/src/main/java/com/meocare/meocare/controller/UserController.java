package com.meocare.meocare.controller;

import com.meocare.meocare.dto.CreateStaffUserRequest;
import com.meocare.meocare.dto.UpdateUserStatusRequest;
import com.meocare.meocare.dto.UserResponse;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.UserService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserResponse>> getUsers(
            @RequestParam(required = false) RoleName role,
            @RequestParam(required = false) UserStatus status) {
        return ResponseEntity.ok(userService.getUsers(role, status));
    }

    @GetMapping("/veterinarians")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIAN', 'CUSTOMER')")
    public ResponseEntity<List<UserResponse>> getActiveVeterinarians() {
        return ResponseEntity.ok(userService.getActiveVeterinarians());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @GetMapping("/by-email")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserByEmail(@RequestParam String email) {
        return ResponseEntity.ok(userService.getUserByEmail(email));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> createStaffUser(@Valid @RequestBody CreateStaffUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createStaffUser(request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        Long actorId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(userService.updateUserStatus(id, request.status(), actorId));
    }
}
