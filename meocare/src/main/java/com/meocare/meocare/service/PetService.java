package com.meocare.meocare.service;

import com.meocare.meocare.dto.PetImageRequest;
import com.meocare.meocare.dto.PetImageResponse;
import com.meocare.meocare.dto.PetRequest;
import com.meocare.meocare.dto.PetResponse;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.PetImage;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.PetImageRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
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
    public PetResponse createPet(Long ownerId, PetRequest request) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Người dùng", ownerId));

        Pet pet = new Pet();
        pet.setOwner(owner);
        applyRequest(pet, request);

        return PetResponse.from(petRepository.save(pet));
    }

    public List<PetResponse> getAllPets() {
        return petRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(PetResponse::from).toList();
    }

    public List<PetResponse> getPetsByOwner(Long ownerId) {
        if (!userRepository.existsById(ownerId)) {
            throw new ResourceNotFoundException("Người dùng", ownerId);
        }
        return petRepository.findByOwner_IdOrderByCreatedAtDesc(ownerId)
                .stream().map(PetResponse::from).toList();
    }

    public PetResponse getPetById(Long id) {
        return PetResponse.from(findPet(id));
    }

    @Transactional
    public PetResponse updatePet(Long id, PetRequest request) {
        Pet pet = findPet(id);
        applyRequest(pet, request);
        return PetResponse.from(petRepository.save(pet));
    }

    @Transactional
    public void deletePet(Long id) {
        Pet pet = findPet(id);

        // Pet đang được lịch hẹn / hồ sơ bệnh án tham chiếu thì không được xóa (tránh vi phạm khóa ngoại).
        if (!appointmentRepository.findByPet_IdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new BadRequestException("Không thể xóa thú cưng đã có lịch hẹn");
        }
        if (!medicalRecordRepository.findByPet_IdOrderByCreatedAtDesc(id).isEmpty()) {
            throw new BadRequestException("Không thể xóa thú cưng đã có hồ sơ bệnh án");
        }

        // Ảnh thuộc sở hữu của Pet nên xóa cùng.
        petImageRepository.deleteAll(petImageRepository.findByPet_IdOrderByCreatedAtDesc(id));
        petRepository.delete(pet);
    }

    @Transactional
    public PetImageResponse addPetImage(Long petId, PetImageRequest request) {
        Pet pet = findPet(petId);

        PetImage image = new PetImage();
        image.setPet(pet);
        image.setImageUrl(request.imageUrl().trim());

        return PetImageResponse.from(petImageRepository.save(image));
    }

    public List<PetImageResponse> getPetImages(Long petId) {
        if (!petRepository.existsById(petId)) {
            throw new ResourceNotFoundException("Thú cưng", petId);
        }
        return petImageRepository.findByPet_IdOrderByCreatedAtDesc(petId)
                .stream().map(PetImageResponse::from).toList();
    }

    private Pet findPet(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Thú cưng", id));
    }

    private void applyRequest(Pet pet, PetRequest request) {
        pet.setName(request.name().trim());
        pet.setSpecies(request.species().trim());
        pet.setBreed(request.breed());
        pet.setGender(request.gender());
        pet.setDateOfBirth(request.dateOfBirth());
        pet.setWeight(request.weight());
        pet.setDescription(request.description());
    }
}
