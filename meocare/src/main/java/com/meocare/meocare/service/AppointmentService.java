package com.meocare.meocare.service;

import com.meocare.meocare.dto.AppointmentConfirmRequest;
import com.meocare.meocare.dto.AppointmentImageResponse;
import com.meocare.meocare.dto.AppointmentRequest;
import com.meocare.meocare.dto.AppointmentResponse;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.AppointmentImage;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.entity.enums.ProposalStatus;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.entity.enums.UserStatus;
import com.meocare.meocare.exception.BadRequestException;
import com.meocare.meocare.exception.ConflictException;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentImageRepository;
import com.meocare.meocare.repository.AppointmentProposalRepository;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import com.meocare.meocare.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppointmentService {

    /** Allowed appointment lifecycle state transitions. */
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(AppointmentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(AppointmentStatus.PENDING, EnumSet.of(
                AppointmentStatus.RESCHEDULE_PROPOSED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.REJECTED,
                AppointmentStatus.CANCELLED
        ));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.RESCHEDULE_PROPOSED, EnumSet.of(
                AppointmentStatus.RESCHEDULE_PROPOSED,
                AppointmentStatus.CONFIRMED,
                AppointmentStatus.REJECTED,
                AppointmentStatus.CANCELLED
        ));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.CONFIRMED, EnumSet.of(
                AppointmentStatus.RESCHEDULE_PROPOSED,
                AppointmentStatus.COMPLETED,
                AppointmentStatus.CANCELLED
        ));
    }

    private final AppointmentRepository appointmentRepository;
    private final AppointmentImageRepository appointmentImageRepository;
    private final AppointmentProposalRepository appointmentProposalRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request, CustomUserDetails principal) {
        Pet pet = petRepository.findWithOwnerById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet", request.petId()));

        User customer = pet.getOwner();
        if (customer == null) {
            throw new BadRequestException("Pet does not have an assigned owner");
        }
        if (principal.getRole() == RoleName.CUSTOMER && !customer.getId().equals(principal.getId())) {
            throw new AccessDeniedException("Customers can only request appointments for their own pets");
        }
        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Customer account is not active");
        }

        validateTimeRange(request.requestedStart(), request.requestedEnd(), "requested");

        Appointment appointment = new Appointment();
        appointment.setPet(pet);
        appointment.setCustomer(customer);
        if (request.veterinarianId() != null) {
            appointment.setVeterinarian(findActiveVeterinarian(request.veterinarianId()));
        }
        appointment.setRequestedStart(request.requestedStart());
        appointment.setRequestedEnd(request.requestedEnd());
        appointment.setSymptoms(request.symptoms());
        appointment.setStatus(AppointmentStatus.PENDING);

        Appointment saved = appointmentRepository.save(appointment);

        List<AppointmentImageResponse> savedImages = List.of();
        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            List<AppointmentImage> images = request.imageUrls().stream()
                    .map(url -> {
                        AppointmentImage image = new AppointmentImage();
                        image.setAppointment(saved);
                        image.setImageUrl(url.trim());
                        return image;
                    })
                    .toList();
            savedImages = appointmentImageRepository.saveAll(images).stream()
                    .map(AppointmentImageResponse::from)
                    .toList();
        }

        return AppointmentResponse.from(saved, savedImages);
    }

    public List<AppointmentResponse> getAppointments(Long customerId,
                                                     Long veterinarianId,
                                                     AppointmentStatus status,
                                                     CustomUserDetails principal) {
        List<Appointment> appointments;
        if (principal.getRole() == RoleName.CUSTOMER) {
            if (customerId != null && !customerId.equals(principal.getId())) {
                throw new AccessDeniedException("Customers can only view their own appointments");
            }
            appointments = appointmentRepository.findFiltered(principal.getId(), veterinarianId, status);
        } else if (principal.getRole() == RoleName.VETERINARIAN) {
            if (veterinarianId != null && !veterinarianId.equals(principal.getId())) {
                throw new AccessDeniedException("Veterinarians can only filter by their own schedule");
            }
            if (veterinarianId != null) {
                appointments = appointmentRepository.findFiltered(customerId, principal.getId(), status);
            } else {
                appointments = appointmentRepository.findForVeterinarianQueue(principal.getId(), customerId, status);
            }
        } else {
            appointments = appointmentRepository.findFiltered(customerId, veterinarianId, status);
        }
        return toResponsesWithImages(appointments);
    }

    public AppointmentResponse getAppointmentById(Long id, CustomUserDetails principal) {
        Appointment appointment = getDetailEntity(id);
        assertViewAccess(appointment, principal);
        return toResponseWithImages(appointment);
    }

    @Transactional
    public AppointmentResponse assignVeterinarian(Long id, Long veterinarianId, CustomUserDetails principal) {
        Appointment appointment = getDetailEntity(id);
        if (isTerminal(appointment.getStatus())) {
            throw new ConflictException("Cannot assign a veterinarian to a " + appointment.getStatus() + " appointment");
        }

        if (principal.getRole() == RoleName.VETERINARIAN) {
            if (!principal.getId().equals(veterinarianId)) {
                throw new AccessDeniedException("Veterinarians can only assign appointments to themselves");
            }
            if (appointment.getVeterinarian() != null
                    && !appointment.getVeterinarian().getId().equals(principal.getId())) {
                throw new AccessDeniedException("Appointment is already assigned to another veterinarian");
            }
        } else if (principal.getRole() != RoleName.ADMIN) {
            throw new AccessDeniedException("Only clinic administrators or veterinarians can assign a veterinarian");
        }

        User veterinarian = findActiveVeterinarian(veterinarianId);
        if (appointment.getStatus() == AppointmentStatus.CONFIRMED
                && appointment.getConfirmedStart() != null
                && appointment.getConfirmedEnd() != null) {
            assertNoVeterinarianConflict(
                    veterinarian.getId(),
                    appointment.getConfirmedStart(),
                    appointment.getConfirmedEnd(),
                    appointment.getId()
            );
        }

        appointment.setVeterinarian(veterinarian);
        return toResponseWithImages(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse confirmAppointment(Long id,
                                                  AppointmentConfirmRequest request,
                                                  CustomUserDetails principal) {
        Appointment appointment = getDetailEntity(id);
        assertClinicalActionAccess(appointment, principal);
        assertTransition(appointment.getStatus(), AppointmentStatus.CONFIRMED);

        LocalDateTime start = request != null ? request.confirmedStart() : null;
        LocalDateTime end = request != null ? request.confirmedEnd() : null;
        if ((start == null) != (end == null)) {
            throw new BadRequestException("Both confirmedStart and confirmedEnd must be provided together");
        }
        if (start == null) {
            start = appointment.getRequestedStart();
            end = appointment.getRequestedEnd();
        }
        validateTimeRange(start, end, "confirmed");

        User assignedVet = appointment.getVeterinarian();
        if (request != null && request.veterinarianId() != null) {
            if (principal.getRole() == RoleName.VETERINARIAN
                    && !principal.getId().equals(request.veterinarianId())) {
                throw new AccessDeniedException("Veterinarians cannot assign appointments to another veterinarian");
            }
            assignedVet = findActiveVeterinarian(request.veterinarianId());
            appointment.setVeterinarian(assignedVet);
        } else if (assignedVet == null && principal.getRole() == RoleName.VETERINARIAN) {
            assignedVet = findActiveVeterinarian(principal.getId());
            appointment.setVeterinarian(assignedVet);
        }

        if (assignedVet != null) {
            assertNoVeterinarianConflict(assignedVet.getId(), start, end, appointment.getId());
        }

        appointment.setConfirmedStart(start);
        appointment.setConfirmedEnd(end);
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        declinePendingProposals(appointment.getId());

        return toResponseWithImages(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse rejectAppointment(Long id, CustomUserDetails principal) {
        Appointment appointment = getDetailEntity(id);
        assertClinicalActionAccess(appointment, principal);
        assertTransition(appointment.getStatus(), AppointmentStatus.REJECTED);

        appointment.setStatus(AppointmentStatus.REJECTED);
        declinePendingProposals(appointment.getId());

        return toResponseWithImages(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id, CustomUserDetails principal) {
        Appointment appointment = getDetailEntity(id);
        assertViewAccess(appointment, principal);
        assertTransition(appointment.getStatus(), AppointmentStatus.CANCELLED);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        declinePendingProposals(appointment.getId());

        return toResponseWithImages(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus, CustomUserDetails principal) {
        if (newStatus == AppointmentStatus.CONFIRMED) {
            return confirmAppointment(id, null, principal);
        }
        if (newStatus == AppointmentStatus.REJECTED) {
            return rejectAppointment(id, principal);
        }
        if (newStatus == AppointmentStatus.CANCELLED) {
            return cancelAppointment(id, principal);
        }
        if (newStatus == AppointmentStatus.RESCHEDULE_PROPOSED) {
            throw new BadRequestException("Use the appointment proposal endpoint to propose a reschedule");
        }

        Appointment appointment = getDetailEntity(id);
        assertClinicalActionAccess(appointment, principal);
        assertTransition(appointment.getStatus(), newStatus);
        appointment.setStatus(newStatus);
        return toResponseWithImages(appointmentRepository.save(appointment));
    }

    // --------------------------------------------------------- shared helpers

    public User findActiveVeterinarian(Long vetId) {
        User user = userRepository.findWithRoleById(vetId)
                .orElseThrow(() -> new ResourceNotFoundException("Veterinarian", vetId));
        if (user.getRole() == null || user.getRole().getName() != RoleName.VETERINARIAN) {
            throw new BadRequestException("Selected user is not a veterinarian");
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ConflictException("Selected veterinarian account is not active");
        }
        return user;
    }

    public void assertNoVeterinarianConflict(Long veterinarianId,
                                             LocalDateTime start,
                                             LocalDateTime end,
                                             Long excludeAppointmentId) {
        if (veterinarianId == null) {
            return;
        }
        long excludeId = excludeAppointmentId != null ? excludeAppointmentId : 0L;
        boolean conflict = appointmentRepository.existsVeterinarianConflict(
                veterinarianId,
                AppointmentStatus.CONFIRMED,
                start,
                end,
                excludeId
        );
        if (conflict) {
            throw new ConflictException(
                    "Veterinarian already has a confirmed appointment overlapping the selected time slot");
        }
    }

    public void assertViewAccess(Appointment appointment, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.CUSTOMER) {
            if (appointment.getCustomer() == null || !appointment.getCustomer().getId().equals(principal.getId())) {
                throw new AccessDeniedException("You do not have permission to access this appointment");
            }
            return;
        }
        if (principal.getRole() == RoleName.VETERINARIAN) {
            if (appointment.getVeterinarian() != null
                    && !appointment.getVeterinarian().getId().equals(principal.getId())) {
                throw new AccessDeniedException("This appointment is assigned to another veterinarian");
            }
        }
    }

    private void assertClinicalActionAccess(Appointment appointment, CustomUserDetails principal) {
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }
        if (principal.getRole() == RoleName.VETERINARIAN) {
            if (appointment.getVeterinarian() != null
                    && !appointment.getVeterinarian().getId().equals(principal.getId())) {
                throw new AccessDeniedException("This appointment is assigned to another veterinarian");
            }
            return;
        }
        throw new AccessDeniedException("Only clinic staff can perform this appointment action");
    }

    private Appointment getDetailEntity(Long id) {
        return appointmentRepository.findDetailById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
    }

    private void declinePendingProposals(Long appointmentId) {
        appointmentProposalRepository.findByAppointment_IdAndStatus(appointmentId, ProposalStatus.PENDING)
                .forEach(p -> p.setStatus(ProposalStatus.DECLINED));
    }

    private boolean isTerminal(AppointmentStatus status) {
        return status == AppointmentStatus.REJECTED
                || status == AppointmentStatus.CANCELLED
                || status == AppointmentStatus.COMPLETED;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end, String label) {
        if (start == null || end == null) {
            throw new BadRequestException("Start and end times for " + label + " slot are required");
        }
        if (!end.isAfter(start)) {
            throw new BadRequestException("End time for " + label + " slot must be after start time");
        }
    }

    private void assertTransition(AppointmentStatus from, AppointmentStatus to) {
        Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new ConflictException("Cannot transition appointment status from " + from + " to " + to);
        }
    }

    private AppointmentResponse toResponseWithImages(Appointment appointment) {
        List<AppointmentImageResponse> images = appointmentImageRepository
                .findByAppointment_IdOrderByCreatedAtDesc(appointment.getId())
                .stream()
                .map(AppointmentImageResponse::from)
                .toList();
        return AppointmentResponse.from(appointment, images);
    }

    private List<AppointmentResponse> toResponsesWithImages(List<Appointment> appointments) {
        if (appointments.isEmpty()) {
            return List.of();
        }
        List<Long> ids = appointments.stream().map(Appointment::getId).toList();
        Map<Long, List<AppointmentImageResponse>> imagesByAppointmentId = appointmentImageRepository
                .findByAppointment_IdInOrderByCreatedAtDesc(ids)
                .stream()
                .map(AppointmentImageResponse::from)
                .collect(Collectors.groupingBy(AppointmentImageResponse::appointmentId));

        return appointments.stream()
                .map(a -> AppointmentResponse.from(
                        a, imagesByAppointmentId.getOrDefault(a.getId(), Collections.emptyList())))
                .toList();
    }
}
