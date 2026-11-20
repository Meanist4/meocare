package com.meocare.meocare.controller;

import com.meocare.meocare.dto.HealthUpdateImageResponse;
import com.meocare.meocare.dto.HealthUpdateRequest;
import com.meocare.meocare.dto.HealthUpdateResponse;
import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.HealthUpdateService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthUpdateController {

    private final HealthUpdateService healthUpdateService;

    @PostMapping("/api/medical-records/{medicalRecordId}/health-updates")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<HealthUpdateResponse> create(
            @PathVariable Long medicalRecordId,
            @Valid @RequestBody HealthUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthUpdateService.createHealthUpdate(medicalRecordId, request, principal));
    }

    @GetMapping("/api/medical-records/{medicalRecordId}/health-updates")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<HealthUpdateResponse>> getByMedicalRecordId(
            @PathVariable Long medicalRecordId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(healthUpdateService.getHealthUpdatesByMedicalRecordId(medicalRecordId, principal));
    }

    @GetMapping("/api/health-updates")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<HealthUpdateResponse>> getTimeline(
            @RequestParam(required = false) Long petId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long medicalRecordId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(healthUpdateService.getTimeline(petId, customerId, medicalRecordId, principal));
    }

    @PostMapping("/api/health-updates/{healthUpdateId}/images")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<HealthUpdateImageResponse>> addImages(
            @PathVariable Long healthUpdateId,
            @Valid @RequestBody ImageUrlsRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthUpdateService.addImages(healthUpdateId, request, principal));
    }

    @GetMapping("/api/health-updates/{healthUpdateId}/images")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<HealthUpdateImageResponse>> getImages(
            @PathVariable Long healthUpdateId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(healthUpdateService.getImages(healthUpdateId, principal));
    }

    @DeleteMapping("/api/health-updates/{healthUpdateId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('VETERINARIAN', 'ADMIN')")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long healthUpdateId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        healthUpdateService.deleteImage(healthUpdateId, imageId, principal);
        return ResponseEntity.noContent().build();
    }
}
