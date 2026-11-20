package com.meocare.meocare.controller;

import com.meocare.meocare.dto.ProposalRequest;
import com.meocare.meocare.dto.ProposalResponse;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.AppointmentProposalService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AppointmentProposalController {

    private final AppointmentProposalService proposalService;

    @PostMapping("/api/appointment-proposals")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<ProposalResponse> create(
            @Valid @RequestBody ProposalRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.createProposal(null, request, principal));
    }

    @PostMapping("/api/appointments/{appointmentId}/proposals")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<ProposalResponse> createForAppointment(
            @PathVariable Long appointmentId,
            @Valid @RequestBody ProposalRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(proposalService.createProposal(appointmentId, request, principal));
    }

    @GetMapping({
            "/api/appointment-proposals/appointment/{appointmentId}",
            "/api/appointments/{appointmentId}/proposals"
    })
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<ProposalResponse>> listByAppointment(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(proposalService.getProposalsByAppointmentId(appointmentId, principal));
    }

    @PatchMapping("/api/appointment-proposals/{id}/accept")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<ProposalResponse> accept(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(proposalService.acceptProposal(null, id, principal));
    }

    @PatchMapping("/api/appointments/{appointmentId}/proposals/{id}/accept")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<ProposalResponse> acceptForAppointment(
            @PathVariable Long appointmentId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(proposalService.acceptProposal(appointmentId, id, principal));
    }

    @PatchMapping("/api/appointment-proposals/{id}/decline")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<ProposalResponse> decline(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(proposalService.declineProposal(null, id, principal));
    }

    @PatchMapping("/api/appointments/{appointmentId}/proposals/{id}/decline")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<ProposalResponse> declineForAppointment(
            @PathVariable Long appointmentId,
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(proposalService.declineProposal(appointmentId, id, principal));
    }
}
