package com.meocare.meocare.controller;

import com.meocare.meocare.dto.PetImageRequest;
import com.meocare.meocare.dto.PetImageResponse;
import com.meocare.meocare.dto.PetRequest;
import com.meocare.meocare.dto.PetResponse;
import com.meocare.meocare.service.PetService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Giai đoạn chưa có JWT: ownerId được truyền qua query param.
 * Khi có JWT, thay bằng id của người dùng đăng nhập và kiểm tra quyền sở hữu thú cưng.
 */
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ResponseEntity<PetResponse> createPet(@RequestParam Long ownerId,
                                                 @Valid @RequestBody PetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(ownerId, request));
    }

    /** Không truyền ownerId: trả toàn bộ thú cưng. */
    @GetMapping
    public ResponseEntity<List<PetResponse>> getPets(@RequestParam(required = false) Long ownerId) {
        List<PetResponse> pets = ownerId != null ? petService.getPetsByOwner(ownerId) : petService.getAllPets();
        return ResponseEntity.ok(pets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PetResponse> getPetById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PetResponse> updatePet(@PathVariable Long id,
                                                 @Valid @RequestBody PetRequest request) {
        return ResponseEntity.ok(petService.updatePet(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePet(@PathVariable Long id) {
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{petId}/images")
    public ResponseEntity<PetImageResponse> addPetImage(@PathVariable Long petId,
                                                        @Valid @RequestBody PetImageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.addPetImage(petId, request));
    }

    @GetMapping("/{petId}/images")
    public ResponseEntity<List<PetImageResponse>> getPetImages(@PathVariable Long petId) {
        return ResponseEntity.ok(petService.getPetImages(petId));
    }
}
