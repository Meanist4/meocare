package com.meocare.meocare.service;

import com.meocare.meocare.dto.PetImageRequest;
import com.meocare.meocare.dto.PetImageResponse;
import com.meocare.meocare.dto.PetRequest;
import com.meocare.meocare.dto.PetResponse;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.PetImage;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.exception.ConflictException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.PetImageRepository;
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
public class PetService {

    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final MedicalRecordRepository medicalRecordRepository;

    @Transactional
    public PetResponse createPet(Long requestedOwnerId, PetRequest request, CustomUserDetails principal) {
        Long resolvedOwnerId;
        if (principal.getRole() == RoleName.CUSTOMER) {
            if (requestedOwnerId != null && !requestedOwnerId.equals(principal.getId())) {
                throw new AccessDeniedException("Customers can only create pets for their own account");
            }
            resolvedOwnerId = principal.getId();
        } else if (principal.getRole() == RoleName.ADMIN) {
            resolvedOwnerId = requestedOwnerId != null ? requestedOwnerId : principal.getId();
        } else {
            throw new AccessDeniedException("Only customers and clinic administrators can create pets");
        }

        User owner = userRepository.findById(resolvedOwnerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", resolvedOwnerId));

        Pet pet = new Pet();
        pet.setOwner(owner);
        applyRequest(pet, request);

        Pet saved = petRepository.save(pet);
        return PetResponse.from(saved, List.of());
    }

    public List<PetResponse> getPets(Long requestedOwnerId, CustomUserDetails principal) {
        List<Pet> pets;
        if (principal.getRole() == RoleName.CUSTOMER) {
            if (requestedOwnerId != null && !requestedOwnerId.equals(principal.getId())) {
                throw new AccessDeniedException("Customers can only view their own pets");
            }
            pets = petRepository.findByOwner_IdOrderByCreatedAtDesc(principal.getId());
        } else {
            if (requestedOwnerId != null) {
                if (!userRepository.existsById(requestedOwnerId)) {
                    throw new ResourceNotFoundException("User", requestedOwnerId);
                }
                pets = petRepository.findByOwner_IdOrderByCreatedAtDesc(requestedOwnerId);
            } else {
                pets = petRepository.findAllByOrderByCreatedAtDesc();
            }
        }
        return toResponsesWithImages(pets);
    }

    public PetResponse getPetById(Long id, CustomUserDetails principal) {
        Pet pet = findPetWithOwner(id);
        assertReadAccess(pet, principal);
        List<PetImageResponse> images = petImageRepository.findByPet_IdOrderByCreatedAtDesc(id)
                .stream()
                .map(PetImageResponse::from)
                .toList();
        return PetResponse.from(pet, images);
    }

    @Transactional
    public PetResponse updatePet(Long id, PetRequest request, CustomUserDetails principal) {
        Pet pet = findPetWithOwner(id);
        assertWriteAccess(pet, principal);
        applyRequest(pet, request);
        Pet saved = petRepository.save(pet);
        List<PetImageResponse> images = petImageRepository.findByPet_IdOrderByCreatedAtDesc(id)
                .stream()
                .map(PetImageResponse::from)
                .toList();
        return PetResponse.from(saved, images);
    }

    @Transactional
    public void deletePet(Long id, CustomUserDetails principal) {
        Pet pet = findPetWithOwner(id);
        assertWriteAccess(pet, principal);

        if (!appointmentRepository.findByPet_IdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new ConflictException("Cannot delete a pet that has existing appointments");
        }
        if (!medicalRecordRepository.findByPet_IdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new ConflictException("Cannot delete a pet that has existing medical records");
        }

        petImageRepository.deleteAll(petImageRepository.findByPet_IdOrderByCreatedAtDesc(id));
        petRepository.delete(pet);
    }

    @Transactional
    public PetImageResponse addPetImage(Long petId, PetImageRequest request, CustomUserDetails principal) {
        Pet pet = findPetWithOwner(petId);
        assertWriteAccess(pet, principal);

        PetImage image = new PetImage();
        image.setPet(pet);
        image.setImageUrl(request.imageUrl().trim());

        return PetImageResponse.from(petImageRepository.save(image));
    }

    public List<PetImageResponse> getPetImages(Long petId, CustomUserDetails principal) {
        Pet pet = findPetWithOwner(petId);
        assertReadAccess(pet, principal);
        return petImageRepository.findByPet_IdOrderByCreatedAtDesc(petId)
                .stream()
                .map(PetImageResponse::from)
                .toList();
    }

    @Transactional
    public void deletePetImage(Long petId, Long imageId, CustomUserDetails principal) {
        Pet pet = findPetWithOwner(petId);
        assertWriteAccess(pet, principal);
        PetImage image = petImageRepository.findByIdAndPet_Id(imageId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet image", imageId));
        petImageRepository.delete(image);
    }

    private Pet findPetWithOwner(Long id) {
        return petRepository.findWithOwnerById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet", id));
    }

    private void assertReadAccess(Pet pet, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.CUSTOMER && !isOwner(pet, principal.getId())) {
            throw new AccessDeniedException("You do not have permission to view this pet");
        }
    }

    private void assertWriteAccess(Pet pet, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.CUSTOMER && isOwner(pet, principal.getId())) {
            return;
        }
        throw new AccessDeniedException("You do not have permission to modify this pet");
    }

    private boolean isOwner(Pet pet, Long userId) {
        return pet.getOwner() != null && userId != null && userId.equals(pet.getOwner().getId());
    }

    private List<PetResponse> toResponsesWithImages(List<Pet> pets) {
        if (pets.isEmpty()) {
            return List.of();
        }
        List<Long> petIds = pets.stream().map(Pet::getId).toList();
        Map<Long, List<PetImageResponse>> imagesByPetId = petImageRepository
                .findByPet_IdInOrderByCreatedAtDesc(petIds)
                .stream()
                .map(PetImageResponse::from)
                .collect(Collectors.groupingBy(PetImageResponse::petId));

        return pets.stream()
                .map(pet -> PetResponse.from(pet, imagesByPetId.getOrDefault(pet.getId(), Collections.emptyList())))
                .toList();
    }

    private void applyRequest(Pet pet, PetRequest request) {
        pet.setName(request.name().trim());
        pet.setSpecies(request.species().trim());
        pet.setBreed(request.breed() != null ? request.breed().trim() : null);
        pet.setGender(request.gender());
        pet.setDateOfBirth(request.dateOfBirth());
        pet.setWeight(request.weight());
        pet.setDescription(request.description());
    }
}
