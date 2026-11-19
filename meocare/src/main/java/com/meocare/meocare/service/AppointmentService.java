package com.meocare.meocare.service;

import com.meocare.meocare.dto.AppointmentConfirmRequest;
import com.meocare.meocare.dto.AppointmentRequest;
import com.meocare.meocare.dto.AppointmentResponse;
import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.Pet;
import com.meocare.meocare.entity.User;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import com.meocare.meocare.entity.enums.UserStatus;
import com.meocare.meocare.exception.ResourceNotFoundException;
import com.meocare.meocare.repository.AppointmentRepository;
import com.meocare.meocare.repository.PetRepository;
import com.meocare.meocare.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    /** Bảng chuyển trạng thái hợp lệ. Trạng thái không có trong bảng coi như không được chuyển đi đâu. */
    private static final Map<AppointmentStatus, Set<AppointmentStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(AppointmentStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(AppointmentStatus.PENDING,
                EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.CANCELLED));
        ALLOWED_TRANSITIONS.put(AppointmentStatus.CONFIRMED,
                EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
    }

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;

    // ------------------------------------------------------------------ create

    @Transactional
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        Pet pet = petRepository.findById(request.petId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy thú cưng với id " + request.petId()));

        User customer = pet.getOwner();
        if (customer == null) {
            throw new IllegalArgumentException("Thú cưng chưa có chủ");
        }
        customer = findActiveUser(customer.getId(), "khách hàng");

        validateTimeRange(request.requestedStart(), request.requestedEnd(), "yêu cầu");

        Appointment appointment = new Appointment();
        appointment.setPet(pet);
        appointment.setCustomer(customer);
        if (request.veterinarianId() != null) {
            appointment.setVeterinarian(findActiveUser(request.veterinarianId(), "bác sĩ thú y"));
        }
        appointment.setRequestedStart(request.requestedStart());
        appointment.setRequestedEnd(request.requestedEnd());
        appointment.setSymptoms(request.symptoms());
        appointment.setStatus(AppointmentStatus.PENDING);

        return toResponse(appointmentRepository.save(appointment));
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public List<AppointmentResponse> getAppointments(Long customerId, Long veterinarianId, AppointmentStatus status) {
        return appointmentRepository.findAll().stream()
                .filter(a -> customerId == null || (a.getCustomer() != null && customerId.equals(a.getCustomer().getId())))
                .filter(a -> veterinarianId == null || (a.getVeterinarian() != null && veterinarianId.equals(a.getVeterinarian().getId())))
                .filter(a -> status == null || status.equals(a.getStatus()))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(Long id) {
        return toResponse(getEntity(id));
    }

    // ----------------------------------------------------------------- confirm

    /** Chốt lịch. request có thể null => dùng đúng khung giờ khách yêu cầu. */
    @Transactional
    public AppointmentResponse confirmAppointment(Long id, AppointmentConfirmRequest request) {
        Appointment appointment = getEntity(id);
        assertTransition(appointment.getStatus(), AppointmentStatus.CONFIRMED);

        LocalDateTime start = request != null ? request.confirmedStart() : null;
        LocalDateTime end = request != null ? request.confirmedEnd() : null;
        if ((start == null) != (end == null)) {
            throw new IllegalArgumentException("Phải truyền đủ cả confirmedStart và confirmedEnd");
        }
        if (start == null) {
            start = appointment.getRequestedStart();
            end = appointment.getRequestedEnd();
        }
        validateTimeRange(start, end, "xác nhận");

        if (request != null && request.veterinarianId() != null) {
            appointment.setVeterinarian(findActiveUser(request.veterinarianId(), "bác sĩ thú y"));
        }
        appointment.setConfirmedStart(start);
        appointment.setConfirmedEnd(end);
        appointment.setStatus(AppointmentStatus.CONFIRMED);

        return toResponse(appointmentRepository.save(appointment));
    }

    // ------------------------------------------------------------ status/cancel

    @Transactional
    public AppointmentResponse updateStatus(Long id, AppointmentStatus newStatus) {
        if (newStatus == AppointmentStatus.CONFIRMED) {
            return confirmAppointment(id, null); // đi qua confirm để luôn có confirmedStart/End
        }
        Appointment appointment = getEntity(id);
        assertTransition(appointment.getStatus(), newStatus);
        appointment.setStatus(newStatus);
        return toResponse(appointmentRepository.save(appointment));
    }

    @Transactional
    public AppointmentResponse cancelAppointment(Long id) {
        return updateStatus(id, AppointmentStatus.CANCELLED);
    }

    // ----------------------------------------------------------------- helpers

    private Appointment getEntity(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch hẹn với id " + id));
    }

    private User findActiveUser(Long userId, String label) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy " + label + " với id " + userId));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalStateException("Tài khoản " + label + " không ở trạng thái ACTIVE");
        }
        return user;
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end, String label) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Thời gian " + label + " không được để trống");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("Thời gian kết thúc " + label + " phải sau thời gian bắt đầu");
        }
    }

    private void assertTransition(AppointmentStatus from, AppointmentStatus to) {
        Set<AppointmentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(from, Set.of());
        if (!allowed.contains(to)) {
            throw new IllegalStateException("Không thể chuyển lịch hẹn từ " + from + " sang " + to);
        }
    }

    private AppointmentResponse toResponse(Appointment a) {
        return AppointmentResponse.from(a);
    }
}
