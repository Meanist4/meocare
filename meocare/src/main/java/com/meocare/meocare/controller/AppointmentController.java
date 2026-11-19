package com.meocare.meocare.controller;

import com.meocare.meocare.dto.AppointmentConfirmRequest;
import com.meocare.meocare.dto.AppointmentRequest;
import com.meocare.meocare.dto.AppointmentResponse;
import com.meocare.meocare.dto.AppointmentStatusUpdateRequest;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.createAppointment(request));
    }

    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> list(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.getAppointments(customerId, veterinarianId, status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getAppointmentById(id));
    }

    @PatchMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirm(
            @PathVariable Long id,
            @RequestBody(required = false) AppointmentConfirmRequest request) {
        return ResponseEntity.ok(appointmentService.confirmAppointment(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AppointmentResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentStatusUpdateRequest request) {
        return ResponseEntity.ok(appointmentService.updateStatus(id, request.status()));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.cancelAppointment(id));
    }
}
