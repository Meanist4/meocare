package com.meocare.meocare.controller;

import com.meocare.meocare.dto.AppointmentAssignRequest;
import com.meocare.meocare.dto.AppointmentConfirmRequest;
import com.meocare.meocare.dto.AppointmentRequest;
import com.meocare.meocare.dto.AppointmentResponse;
import com.meocare.meocare.dto.AppointmentStatusUpdateRequest;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.AppointmentService;
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
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> create(
            @Valid @RequestBody AppointmentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(appointmentService.createAppointment(request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<AppointmentResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) AppointmentStatus status,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.getAppointments(customerId, veterinarianId, status, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id, principal));
    }

    @PatchMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIAN')")
    public ResponseEntity<AppointmentResponse> assignVeterinarian(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentAssignRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.assignVeterinarian(id, request.veterinarianId(), principal));
    }

    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIAN')")
    public ResponseEntity<AppointmentResponse> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) AppointmentConfirmRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id, request, principal));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'VETERINARIAN')")
    public ResponseEntity<AppointmentResponse> reject(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.rejectAppointment(id, principal));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request.status(), principal));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<AppointmentResponse> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id, principal));
    }
}
