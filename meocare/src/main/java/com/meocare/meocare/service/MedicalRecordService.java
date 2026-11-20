package com.meocare.meocare.service;

import com.meocare.meocare.dto.ImageUrlsRequest;
import com.meocare.meocare.dto.MedicalRecordImageResponse;
import com.meocare.meocare.dto.MedicalRecordRequest;
import com.meocare.meocare.dto.MedicalRecordResponse;
import com.meocare.meocare.dto.MedicalRecordUpdateRequest;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.MedicalRecord;
import com.meocare.meocare.entity.MedicalRecordImage;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ConflictException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.MedicalRecordImageRepository;
import com.meocare.meocare.repository.MedicalRecordRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import com.meocare.meocare.security.CustomUserDetails;
import java.math.BigDecimal;
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
public class MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalRecordImageRepository imageRepository;
    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional
    public MedicalRecordResponse createMedicalRecord(MedicalRecordRequest request, CustomUserDetails principal) {
        assertClinicalStaff(principal);

        Appointment appointment = appointmentRepository.findDetailById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", request.appointmentId()));

        if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                && appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw new ConflictException(
                    "Medical record can only be created for a CONFIRMED or COMPLETED appointment");
        }
        if (medicalRecordRepository.existsByAppointment_Id(request.appointmentId())) {
            throw new ConflictException(
                    "Appointment " + request.appointmentId() + " already has a medical record");
        }
        if (appointment.getPet() == null) {
            throw new BadRequestException("Appointment is not linked to a pet");
        }
        Pet pet = petRepository.findWithOwnerById(appointment.getPet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet", appointment.getPet().getId()));

        User veterinarian;
        if (principal.getRole() == RoleName.VETERINARIAN) {
            veterinarian = userRepository.findWithRoleById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", principal.getId()));
            if (appointment.getVeterinarian() == null) {
                appointment.setVeterinarian(veterinarian);
                appointmentRepository.save(appointment);
            }
        } else {
            if (appointment.getVeterinarian() == null) {
                throw new BadRequestException("Appointment must have an assigned veterinarian before creating a medical record");
            }
            veterinarian = userRepository.findWithRoleById(appointment.getVeterinarian().getId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Veterinarian", appointment.getVeterinarian().getId()));
        }

        MedicalRecord record = new MedicalRecord();
        record.setAppointment(appointment);
        record.setPet(pet);
        record.setVeterinarian(veterinarian);
        applyFields(record, request.weight(), request.temperature(), request.examination(),
                request.diagnosis(), request.treatment(), request.advice(), request.notes());

        MedicalRecord saved = medicalRecordRepository.save(record);
        List<MedicalRecordImageResponse> savedImages = saveImages(saved, request.imageUrls());
        return MedicalRecordResponse.from(saved, savedImages);
    }

    public MedicalRecordResponse getMedicalRecordById(Long id, CustomUserDetails principal) {
        MedicalRecord record = findRecordDetail(id);
        assertReadAccess(record, principal);
        return toResponseWithImages(record);
    }

    public MedicalRecordResponse getMedicalRecordByAppointmentId(Long appointmentId, CustomUserDetails principal) {
        if (!appointmentRepository.existsById(appointmentId)) {
            throw new ResourceNotFoundException("Appointment", appointmentId);
        }
        MedicalRecord record = medicalRecordRepository.findDetailByAppointmentId(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Medical record not found for appointment id = " + appointmentId));
        assertReadAccess(record, principal);
        return toResponseWithImages(record);
    }

    public List<MedicalRecordResponse> getMedicalRecords(Long petId,
                                                         Long veterinarianId,
                                                         Long customerId,
                                                         CustomUserDetails principal) {
        Long resolvedCustomerId = customerId;
        if (principal.getRole() == RoleName.CUSTOMER) {
            if (customerId != null && !customerId.equals(principal.getId())) {
                throw new AccessDeniedException("Customers can only view medical records for their own pets");
            }
            if (petId != null) {
                Pet pet = petRepository.findWithOwnerById(petId)
                        .orElseThrow(() -> new ResourceNotFoundException("Pet", petId));
                if (pet.getOwner() == null || !pet.getOwner().getId().equals(principal.getId())) {
                    throw new AccessDeniedException("You do not have permission to view medical records for this pet");
                }
            }
            resolvedCustomerId = principal.getId();
        } else {
            if (petId != null && !petRepository.existsById(petId)) {
                throw new ResourceNotFoundException("Pet", petId);
            }
        }

        List<MedicalRecord> records = medicalRecordRepository.findFiltered(petId, veterinarianId, resolvedCustomerId);
        return toResponsesWithImages(records);
    }

    @Transactional
    public MedicalRecordResponse updateMedicalRecord(Long id,
                                                     MedicalRecordUpdateRequest request,
                                                     CustomUserDetails principal) {
        assertClinicalStaff(principal);
        MedicalRecord record = findRecordDetail(id);
        applyFields(record, request.weight(), request.temperature(), request.examination(),
                request.diagnosis(), request.treatment(), request.advice(), request.notes());
        MedicalRecord saved = medicalRecordRepository.save(record);
        return toResponseWithImages(saved);
    }

    @Transactional
    public List<MedicalRecordImageResponse> addImages(Long medicalRecordId,
                                                      ImageUrlsRequest request,
                                                      CustomUserDetails principal) {
        assertClinicalStaff(principal);
        MedicalRecord record = findRecordDetail(medicalRecordId);
        List<MedicalRecordImage> images = request.imageUrls().stream()
                .map(url -> createImage(record, url))
                .toList();
        return imageRepository.saveAll(images).stream()
                .map(MedicalRecordImageResponse::from)
                .toList();
    }

    public List<MedicalRecordImageResponse> getImages(Long medicalRecordId, CustomUserDetails principal) {
        MedicalRecord record = findRecordDetail(medicalRecordId);
        assertReadAccess(record, principal);
        return imageRepository.findByMedicalRecord_IdOrderByCreatedAtDesc(medicalRecordId).stream()
                .map(MedicalRecordImageResponse::from)
                .toList();
    }

    @Transactional
    public void deleteImage(Long medicalRecordId, Long imageId, CustomUserDetails principal) {
        assertClinicalStaff(principal);
        if (!medicalRecordRepository.existsById(medicalRecordId)) {
            throw new ResourceNotFoundException("Medical record", medicalRecordId);
        }
        MedicalRecordImage image = imageRepository.findByIdAndMedicalRecord_Id(imageId, medicalRecordId)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record image", imageId));
        imageRepository.delete(image);
    }

    private MedicalRecord findRecordDetail(Long id) {
        return medicalRecordRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medical record", id));
    }

    private void assertClinicalStaff(CustomUserDetails principal) {
        if (principal.getRole() != RoleName.VETERINARIAN && principal.getRole() != RoleName.ADMIN) {
            throw new AccessDeniedException("Only veterinarians and clinic administrators can modify medical records");
        }
    }

    private void assertReadAccess(MedicalRecord record, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.CUSTOMER) {
            Long ownerId = (record.getPet() != null && record.getPet().getOwner() != null)
                    ? record.getPet().getOwner().getId()
                    : null;
            if (ownerId == null || !ownerId.equals(principal.getId())) {
                throw new AccessDeniedException("You do not have permission to view this medical record");
            }
        }
    }

    private List<MedicalRecordImageResponse> saveImages(MedicalRecord record, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        List<MedicalRecordImage> images = imageUrls.stream()
                .map(url -> createImage(record, url))
                .toList();
        return imageRepository.saveAll(images).stream()
                .map(MedicalRecordImageResponse::from)
                .toList();
    }

    private MedicalRecordImage createImage(MedicalRecord record, String imageUrl) {
        MedicalRecordImage image = new MedicalRecordImage();
        image.setMedicalRecord(record);
        image.setImageUrl(imageUrl.trim());
        return image;
    }

    private MedicalRecordResponse toResponseWithImages(MedicalRecord record) {
        List<MedicalRecordImageResponse> images = imageRepository
                .findByMedicalRecord_IdOrderByCreatedAtDesc(record.getId())
                .stream()
                .map(MedicalRecordImageResponse::from)
                .toList();
        return MedicalRecordResponse.from(record, images);
    }

    private List<MedicalRecordResponse> toResponsesWithImages(List<MedicalRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> ids = records.stream().map(MedicalRecord::getId).toList();
        Map<Long, List<MedicalRecordImageResponse>> imagesByRecordId = imageRepository
                .findByMedicalRecord_IdInOrderByCreatedAtDesc(ids)
                .stream()
                .map(MedicalRecordImageResponse::from)
                .collect(Collectors.groupingBy(MedicalRecordImageResponse::medicalRecordId));

        return records.stream()
                .map(r -> MedicalRecordResponse.from(
                        r, imagesByRecordId.getOrDefault(r.getId(), Collections.emptyList())))
                .toList();
    }

    private void applyFields(MedicalRecord record, BigDecimal weight,
                             BigDecimal temperature, String examination,
                             String diagnosis, String treatment, String advice, String notes) {
        record.setWeight(weight);
        record.setTemperature(temperature);
        record.setExamination(examination);
        record.setDiagnosis(diagnosis);
        record.setTreatment(treatment);
        record.setAdvice(advice);
        record.setNotes(notes);
    }
}
