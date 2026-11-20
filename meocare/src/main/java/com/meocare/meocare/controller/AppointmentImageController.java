package com.meocare.meocare.controller;

import com.meocare.meocare.dto.AppointmentImageResponse;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.AppointmentImageService;
import com.meocare.meocare.service.AppointmentImageService.ImageRequest;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments/{appointmentId}/images")
@RequiredArgsConstructor
public class AppointmentImageController {

    private final AppointmentImageService imageService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<AppointmentImageResponse> add(
            @PathVariable Long appointmentId,
            @Valid @RequestBody ImageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(imageService.addImage(appointmentId, request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<AppointmentImageResponse>> list(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(imageService.getImages(appointmentId, principal));
    }

    @DeleteMapping("/{imageId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long appointmentId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        imageService.deleteImage(appointmentId, imageId, principal);
        return ResponseEntity.noContent().build();
    }
}
