package com.meocare.meocare.controller;

import com.meocare.meocare.dto.ProposalRequest;
import com.meocare.meocare.dto.ProposalResponse;
import com.meocare.meocare.service.AppointmentProposalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appointment-proposals")
@RequiredArgsConstructor
public class AppointmentProposalController {

    private final AppointmentProposalService proposalService;

    @PostMapping
    public ResponseEntity<ProposalResponse> create(@Valid @RequestBody ProposalRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(proposalService.createProposal(request));
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<List<ProposalResponse>> listByAppointment(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(proposalService.getProposalsByAppointmentId(appointmentId));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<ProposalResponse> accept(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.acceptProposal(id));
    }

    @PatchMapping("/{id}/decline")
    public ResponseEntity<ProposalResponse> decline(@PathVariable Long id) {
        return ResponseEntity.ok(proposalService.declineProposal(id));
    }
}
