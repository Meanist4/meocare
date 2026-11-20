package com.meocare.meocare.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.meocare.meocare.dto.AppointmentConfirmRequest;
import com.meocare.meocare.dto.AppointmentResponse;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.entity.enums.RoleName;
import com.meocare.meocare.exception.ConflictException;
import com.meocare.meocare.repository.AppointmentImageRepository;
import com.meocare.meocare.repository.AppointmentProposalRepository;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import com.meocare.meocare.security.CustomUserDetails;
import com.meocare.meocare.support.BaseUnitTest;
import com.meocare.meocare.support.TestDataFactory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class AppointmentServiceTest extends BaseUnitTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private AppointmentImageRepository appointmentImageRepository;
    @Mock
    private AppointmentProposalRepository appointmentProposalRepository;
    @Mock
    private PetRepository petRepository;
    @Mock
    private UserRepository userRepository;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(
                appointmentRepository,
                appointmentImageRepository,
                appointmentProposalRepository,
                petRepository,
                userRepository
        );
    }

    @Test
    void confirmAppointment_throwsConflictWhenVeterinarianHasOverlappingConfirmedSlot() {
        User customer = TestDataFactory.sampleUser(1L, TestDataFactory.defaultRole(RoleName.CUSTOMER));
        User vet = TestDataFactory.sampleUser(2L, TestDataFactory.defaultRole(RoleName.VETERINARIAN));
        Pet pet = new Pet();
        pet.setId(10L);
        pet.setName("Luna");
        pet.setOwner(customer);

        LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0);
        LocalDateTime end = start.plusMinutes(30);

        Appointment appointment = new Appointment();
        appointment.setId(100L);
        appointment.setPet(pet);
        appointment.setCustomer(customer);
        appointment.setVeterinarian(vet);
        appointment.setRequestedStart(start);
        appointment.setRequestedEnd(end);
        appointment.setStatus(AppointmentStatus.PENDING);

        CustomUserDetails vetPrincipal = CustomUserDetails.from(vet);

        when(appointmentRepository.findDetailById(100L)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.existsVeterinarianConflict(
                eq(2L), eq(AppointmentStatus.CONFIRMED), eq(start), eq(end), eq(100L)))
                .thenReturn(true);

        assertThatThrownBy(() -> appointmentService.confirmAppointment(100L, null, vetPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("overlapping");
    }

    @Test
    void rejectAppointment_transitionsPendingToRejected() {
        User customer = TestDataFactory.sampleUser(1L, TestDataFactory.defaultRole(RoleName.CUSTOMER));
        User vet = TestDataFactory.sampleUser(2L, TestDataFactory.defaultRole(RoleName.VETERINARIAN));
        Pet pet = new Pet();
        pet.setId(10L);
        pet.setName("Milo");
        pet.setOwner(customer);

        Appointment appointment = new Appointment();
        appointment.setId(101L);
        appointment.setPet(pet);
        appointment.setCustomer(customer);
        appointment.setVeterinarian(vet);
        appointment.setStatus(AppointmentStatus.PENDING);

        CustomUserDetails vetPrincipal = CustomUserDetails.from(vet);

        when(appointmentRepository.findDetailById(101L)).thenReturn(Optional.of(appointment));
        when(appointmentProposalRepository.findByAppointment_IdAndStatus(101L, com.meocare.meocare.entity.enums.ProposalStatus.PENDING))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(appointmentImageRepository.findByAppointment_IdOrderByCreatedAtDesc(101L)).thenReturn(List.of());

        AppointmentResponse response = appointmentService.rejectAppointment(101L, vetPrincipal);

        assertThat(response.status()).isEqualTo(AppointmentStatus.REJECTED);
    }

    @Test
    void confirmAppointment_throwsConflictWhenAlreadyCompleted() {
        User vet = TestDataFactory.sampleUser(2L, TestDataFactory.defaultRole(RoleName.VETERINARIAN));
        Appointment appointment = new Appointment();
        appointment.setId(102L);
        appointment.setVeterinarian(vet);
        appointment.setStatus(AppointmentStatus.COMPLETED);

        CustomUserDetails vetPrincipal = CustomUserDetails.from(vet);
        when(appointmentRepository.findDetailById(102L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> appointmentService.confirmAppointment(
                102L, new AppointmentConfirmRequest(null, null, null), vetPrincipal))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot transition");
    }
}
