package com.meocare.meocare.service;

import com.meocare.meocare.dto.HealthUpdateImageResponse;
import com.meocare.meocare.dto.HealthUpdateRequest;
import com.meocare.meocare.dto.HealthUpdateResponse;
import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.entity.HealthUpdate;
import com.meocare.meocare.entity.HealthUpdateImage;
import com.meocare.meocare.entity.MedicalRecord;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.HealthUpdateImageRepository;
import com.meocare.meocare.repository.HealthUpdateRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import com.meocare.meocare.security.CustomUserDetails;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HealthUpdateService {

    private final HealthUpdateRepository healthUpdateRepository;
    private final HealthUpdateImageRepository imageRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional
    public HealthUpdateResponse createHealthUpdate(Long medicalRecordId,
                                                   HealthUpdateRequest request,
                                                   CustomUserDetails principal) {
        assertClinicalStaff(principal);

        MedicalRecord medicalRecord = medicalRecordRepository.findDetailById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record", medicalRecordId));

        User authorVet;
        if (principal.getRole() == RoleName.VETERINARIAN) {
            authorVet = userRepository.findWithRoleById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", principal.getId()));
        } else {
            authorVet = medicalRecord.getVeterinarian() != null
                    ? medicalRecord.getVeterinarian()
                    : userRepository.findWithRoleById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", principal.getId()));
        }

        HealthUpdate healthUpdate = new HealthUpdate();
        healthUpdate.setMedicalRecord(medicalRecord);
        healthUpdate.setVeterinarian(authorVet);
        healthUpdate.setHealthStatus(request.healthStatus().trim());
        healthUpdate.setDescription(request.description());

        HealthUpdate saved = healthUpdateRepository.save(healthUpdate);
        List<HealthUpdateImageResponse> savedImages = saveImages(saved, request.imageUrls());
        return HealthUpdateResponse.from(saved, savedImages);
    }

    public List<HealthUpdateResponse> getHealthUpdatesByMedicalRecordId(Long medicalRecordId,
                                                                        CustomUserDetails principal) {
        MedicalRecord record = medicalRecordRepository.findDetailById(medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record", medicalRecordId));
        assertRecordReadAccess(record, principal);

        List<HealthUpdate> updates = healthUpdateRepository.findTimeline(null, null, medicalRecordId);
        return toResponsesWithImages(updates);
    }

    public List<HealthUpdateResponse> getTimeline(Long petId,
                                                  Long customerId,
                                                  Long medicalRecordId,
                                                  CustomUserDetails principal) {
        Long resolvedCustomerId = customerId;
        if (principal.getRole() == RoleName.CUSTOMER) {
            if (customerId != null && !customerId.equals(principal.getId())) {
                throw new AccessDeniedException("Customers can only view health updates for their own pets");
            }
            if (petId != null) {
                Pet pet = petRepository.findWithOwnerById(petId)
                        .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));
                if (pet.getOwner() == null || !pet.getOwner().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("You do not have permission to view this pet's health timeline");
                }
            }
            if (medicalRecordId != null) {
                MedicalRecord record = medicalRecordRepository.findDetailById(medicalRecordId)
                        .orElseThrow(() -> new ResourceNotFoundException("Medical record", medicalRecordId));
                assertRecordReadAccess(record, principal);
            }
            resolvedCustomerId = principal.getId();
        } else {
            if (petId != null && !petRepository.existsById(petId)) {
                throw new ResourceNotFoundException("Pet", petId);
            }
            if (medicalRecordId != null && !medicalRecordRepository.existsById(medicalRecordId)) {
                throw new ResourceNotFoundException("Medical record", medicalRecordId);
            }
        }

        List<HealthUpdate> updates = healthUpdateRepository.findTimeline(petId, resolvedCustomerId, medicalRecordId);
        return toResponsesWithImages(updates);
    }

    @Transactional
    public List<HealthUpdateImageResponse> addImages(Long healthUpdateId,
                                                     ImageUrlsRequest request,
                                                     CustomUserDetails principal) {
        assertClinicalStaff(principal);
        HealthUpdate healthUpdate = healthUpdateRepository.findDetailById(healthUpdateId)
                .orElseThrow(() -> new ResourceNotFoundException("Health update", healthUpdateId));
        List<HealthUpdateImage> images = request.imageUrls().stream()
                .map(url -> createImage(healthUpdate, url))
                .toList();
        return imageRepository.saveAll(images).stream()
                .map(HealthUpdateImageResponse::from)
                .toList();
    }

    public List<HealthUpdateImageResponse> getImages(Long healthUpdateId, CustomUserDetails principal) {
        HealthUpdate healthUpdate = healthUpdateRepository.findDetailById(healthUpdateId)
                .orElseThrow(() -> new ResourceNotFoundException("Health update", healthUpdateId));
        assertRecordReadAccess(healthUpdate.getMedicalRecord(), principal);

        return imageRepository.findByHealthUpdate_IdOrderByCreatedAtDesc(healthUpdateId).stream()
                .map(HealthUpdateImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteImage(Long healthUpdateId, Long imageId, CustomUserDetails principal) {
        assertClinicalStaff(principal);
        if (!healthUpdateRepository.existsById(healthUpdateId)) {
            throw new ResourceNotFoundException("Health update", healthUpdateId);
        }
        HealthUpdateImage image = imageRepository.findByIdAndHealthUpdate_Id(imageId, healthUpdateId)
                .orElseThrow(() -> new ResourceNotFoundException("Health update image", imageId));
        imageRepository.delete(image);
    }

    private void assertClinicalStaff(CustomUserDetails principal) {
        if (principal.getRole() != RoleName.VETERINARIAN && principal.getRole() != RoleName.ADMIN) {
            throw new AccessDeniedException("Only veterinarians and clinic administrators can post health updates");
        }
    }

    private void assertRecordReadAccess(MedicalRecord record, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.CUSTOMER) {
            Long ownerId = (record != null && record.getPet() != null && record.getPet().getOwner() != null)
                    ? record.getPet().getOwner().getId()
                    : null;
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("You do not have permission to view this health timeline");
            }
        }
    }

    private List<HealthUpdateImageResponse> saveImages(HealthUpdate healthUpdate, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<HealthUpdateImage> images = imageUrls.stream()
                .map(url -> createImage(healthUpdate, url))
                .toList();
        return imageRepository.saveAll(images).stream()
                .map(HealthUpdateImageResponse::from)
                .toList();
    }

    private HealthUpdateImage createImage(HealthUpdate healthUpdate, String imageUrl) {
        HealthUpdateImage image = new HealthUpdateImage();
        image.setHealthUpdate(healthUpdate);
        image.setImageUrl(imageUrl.trim());
        return image;
    }

    private List<HealthUpdateResponse> toResponsesWithImages(List<HealthUpdate> updates) {
        if (updates.isEmpty()) {
            return List.of();
        }
        List<Long> ids = updates.stream().map(HealthUpdate::getId).toList();
        Map<Long, List<HealthUpdateImageResponse>> imagesByUpdateId = imageRepository
                .findByHealthUpdate_IdInOrderByCreatedAtDesc(ids)
                .stream()
                .map(HealthUpdateImageResponse::from)
                .collect(Collectors.groupingBy(HealthUpdateImageResponse::healthUpdateId));

        return updates.stream()
                .map(h -> HealthUpdateResponse.from(
                        h, imagesByUpdateId.getOrDefault(h.getId(), Collections.emptyList())))
                .toList();
    }
}
