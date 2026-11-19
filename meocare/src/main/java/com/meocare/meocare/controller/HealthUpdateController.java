package com.meocare.meocare.controller;

import com.meocare.meocare.dto.HealthUpdateImageResponse;
import com.meocare.meocare.dto.HealthUpdateRequest;
import com.meocare.meocare.dto.HealthUpdateResponse;
import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.service.HealthUpdateService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class HealthUpdateController {

    private final HealthUpdateService healthUpdateService;

    @PostMapping("/api/medical-records/{medicalRecordId}/health-updates")
    public ResponseEntity<HealthUpdateResponse> create(
            @PathVariable Long medicalRecordId,
            @Valid @RequestBody HealthUpdateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthUpdateService.createHealthUpdate(medicalRecordId, request));
    }

    @GetMapping("/api/medical-records/{medicalRecordId}/health-updates")
    public ResponseEntity<List<HealthUpdateResponse>> getByMedicalRecordId(
            @PathVariable Long medicalRecordId) {
        return ResponseEntity.ok(healthUpdateService.getHealthUpdatesByMedicalRecordId(medicalRecordId));
    }

    @PostMapping("/api/health-updates/{healthUpdateId}/images")
    public ResponseEntity<List<HealthUpdateImageResponse>> addImages(
            @PathVariable Long healthUpdateId,
            @Valid @RequestBody ImageUrlsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(healthUpdateService.addImages(healthUpdateId, request));
    }

    @GetMapping("/api/health-updates/{healthUpdateId}/images")
    public ResponseEntity<List<HealthUpdateImageResponse>> getImages(@PathVariable Long healthUpdateId) {
        return ResponseEntity.ok(healthUpdateService.getImages(healthUpdateId));
    }
}
