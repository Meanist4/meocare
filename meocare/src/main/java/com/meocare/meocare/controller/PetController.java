package com.meocare.meocare.controller;

import com.meocare.meocare.dto.PetImageRequest;
import com.meocare.meocare.dto.PetImageResponse;
import com.meocare.meocare.dto.PetRequest;
import com.meocare.meocare.dto.PetResponse;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.service.PetService;
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
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PetResponse> createPet(
            @RequestParam(required = false) Long ownerId,
            @Valid @RequestBody PetRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(petService.createPet(ownerId, request, principal));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<PetResponse>> getPets(
            @RequestParam(required = false) Long ownerId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(petService.getPets(ownerId, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<PetResponse> getPetById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(petService.getPetById(id, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable Long id,
            @Valid @RequestBody PetRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(petService.updatePet(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> deletePet(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        petService.deletePet(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{petId}/images")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<PetImageResponse> addPetImage(
            @PathVariable Long petId,
            @Valid @RequestBody PetImageRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(petService.addPetImage(petId, request, principal));
    }

    @GetMapping("/{petId}/images")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'VETERINARIAN', 'ADMIN')")
    public ResponseEntity<List<PetImageResponse>> getPetImages(
            @PathVariable Long petId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(petService.getPetImages(petId, principal));
    }

    @DeleteMapping("/{petId}/images/{imageId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<Void> deletePetImage(
            @PathVariable Long petId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal CustomUserDetails principal) {
        petService.deletePetImage(petId, imageId, principal);
        return ResponseEntity.noContent().build();
    }
}
