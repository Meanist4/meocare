package com.meocare.meocare.repository;

import com.meocare.meocare.entity.Appointment;
import com.meocare.meocare.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    List<Appointment> findByCustomer_IdOrderByCreatedAtDesc(Long customerId);

    List<Appointment> findByVeterinarian_IdOrderByCreatedAtDesc(Long veterinarianId);

    List<Appointment> findByPet_IdOrderByCreatedAtDesc(Long petId);

    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);

    Page<Appointment> findByVeterinarian_IdAndStatus(Long veterinarianId, AppointmentStatus status, Pageable pageable);

    Optional<Appointment> findByIdAndCustomer_Id(Long id, Long customerId);

    Optional<Appointment> findByIdAndVeterinarian_Id(Long id, Long veterinarianId);

    /** Nạp sẵn pet, customer, veterinarian cho màn hình chi tiết. */
    @Query("""
            select a from Appointment a
            join fetch a.pet
            join fetch a.customer
            left join fetch a.veterinarian
            where a.id = :id
            """)
    Optional<Appointment> findDetailById(@Param("id") Long id);

    /**
     * Kiểm tra bác sĩ bị trùng lịch (chỉ xét lịch có trạng thái {@code status}, thường là CONFIRMED).
     * {@code excludeId}: id lịch cần bỏ qua khi cập nhật; truyền 0L nếu không có.
     */
    @Query("""
            select count(a) > 0 from Appointment a
            where a.veterinarian.id = :veterinarianId
              and a.status = :status
              and a.id <> :excludeId
              and a.confirmedStart < :end
              and a.confirmedEnd > :start
            """)
    boolean existsVeterinarianConflict(@Param("veterinarianId") Long veterinarianId,
                                       @Param("status") AppointmentStatus status,
                                       @Param("start") LocalDateTime start,
                                       @Param("end") LocalDateTime end,
                                       @Param("excludeId") Long excludeId);

    /** Lịch của bác sĩ trong một khoảng thời gian (xem lịch ngày/tuần). */
    @Query("""
            select a from Appointment a
            where a.veterinarian.id = :veterinarianId
              and a.status = :status
              and a.confirmedStart >= :from
              and a.confirmedStart < :to
            order by a.confirmedStart
            """)
    List<Appointment> findVeterinarianSchedule(@Param("veterinarianId") Long veterinarianId,
                                               @Param("status") AppointmentStatus status,
                                               @Param("from") LocalDateTime from,
                                               @Param("to") LocalDateTime to);
}
