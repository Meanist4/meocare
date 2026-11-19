package com.meocare.meocare.controller;

import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.dto.MedicalRecordImageResponse;
import com.meocare.meocare.dto.MedicalRecordRequest;
import com.meocare.meocare.dto.MedicalRecordResponse;
import com.meocare.meocare.dto.MedicalRecordUpdateRequest;
import com.meocare.meocare.service.MedicalRecordService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;

    @PostMapping
    public ResponseEntity<MedicalRecordResponse> create(@Valid @RequestBody MedicalRecordRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.createMedicalRecord(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordById(id));
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public ResponseEntity<MedicalRecordResponse> getByAppointmentId(@PathVariable Long appointmentId) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordByAppointmentId(appointmentId));
    }

    @GetMapping
    public ResponseEntity<List<MedicalRecordResponse>> getByPetId(@RequestParam Long petId) {
        return ResponseEntity.ok(medicalRecordService.getMedicalRecordsByPetId(petId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MedicalRecordResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody MedicalRecordUpdateRequest request) {
        return ResponseEntity.ok(medicalRecordService.updateMedicalRecord(id, request));
    }

    @PostMapping("/{medicalRecordId}/images")
    public ResponseEntity<List<MedicalRecordImageResponse>> addImages(
            @PathVariable Long medicalRecordId,
            @Valid @RequestBody ImageUrlsRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(medicalRecordService.addImages(medicalRecordId, request));
    }

    @GetMapping("/{medicalRecordId}/images")
    public ResponseEntity<List<MedicalRecordImageResponse>> getImages(@PathVariable Long medicalRecordId) {
        return ResponseEntity.ok(medicalRecordService.getImages(medicalRecordId));
    }
}
