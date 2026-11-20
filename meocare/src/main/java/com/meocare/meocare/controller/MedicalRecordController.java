package com.meocare.meocare.controller;

import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.dto.MedicalRecordImageResponse;
import com.meocare.meocare.dto.MedicalRecordRequest;
import com.meocare.meocare.dto.MedicalRecordResponse;
import com.meocare.meocare.dto.MedicalRecordUpdateRequest;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.MedicalRecordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> create(
            @Valid @RequestBody MedicalRecordRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.createMedicalRecord(request, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordById(id, principal));
    }

    @GetMapping("/by-appointment/{appointmentId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> getByAppointmentId(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordByAppointmentId(appointmentId, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<MedicalRecordResponse>> list(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long veterinarianId,
            @RequestParam(required = false) Long customerId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecords(petId, veterinarianId, customerId, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<MedicalRecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MedicalRecordUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(medicalRecordService.updateMedicalRecord(id, request, principal));
    }

    @PostMapping("/{medicalRecordId}/images")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<MedicalRecordImageResponse>> addImages(
            @PathVariable Long medicalRecordId,
            @Valid @RequestBody ImageUrlsRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.addImages(medicalRecordId, request, principal));
    }

    @GetMapping("/{medicalRecordId}/images")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<MedicalRecordImageResponse>> getImages(
            @PathVariable Long medicalRecordId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(medicalRecordService.getImages(medicalRecordId, principal));
    }

    @DeleteMapping("/{medicalRecordId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long medicalRecordId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        medicalRecordService.deleteImage(medicalRecordId, imageId, principal);
        return ResponseEntity.noContent().build();
    }
}
