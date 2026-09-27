package com.cheche.facility.repository;

import com.cheche.facility.domain.Reservation;
import com.cheche.facility.domain.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByUserIdOrderByReservationDateDescStartTimeDesc(Long userId);

    boolean existsByFacilityIdAndReservationDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long facilityId, LocalDate reservationDate, ReservationStatus status,
            LocalTime endTime, LocalTime startTime);

    List<Reservation> findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
            Long facilityId, LocalDate reservationDate, ReservationStatus status);
}
