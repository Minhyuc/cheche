package com.cheche.facility.repository;

import com.cheche.facility.domain.Reservation;
import com.cheche.facility.domain.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findAllByUserIdOrderByReservationDateDescStartTimeDesc(Long userId);

    boolean existsByFacilityIdAndReservationDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
            Long facilityId, LocalDate reservationDate, ReservationStatus status,
            LocalTime endTime, LocalTime startTime);

    @Query("select coalesce(sum(r.participantCount), 0) from Reservation r " +
            "where r.facilityId = :facilityId and r.reservationDate = :date and r.status = :status " +
            "and r.startTime < :endTime and r.endTime > :startTime")
    int sumParticipantsForSlot(@Param("facilityId") Long facilityId,
                               @Param("date") LocalDate date,
                               @Param("status") ReservationStatus status,
                               @Param("endTime") LocalTime endTime,
                               @Param("startTime") LocalTime startTime);

    List<Reservation> findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
            Long facilityId, LocalDate reservationDate, ReservationStatus status);
}
