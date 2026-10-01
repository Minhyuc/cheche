package com.cheche.facility.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.cheche.facility.domain.*;
import com.cheche.facility.dto.ReservationCreateRequest;
import com.cheche.facility.repository.FacilityRepository;
import com.cheche.facility.repository.ReservationRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class ReservationServiceTest {
    private ReservationRepository reservationRepository;
    private FacilityRepository facilityRepository;
    private ReservationService service;
    private Facility facility;

    @BeforeEach
    void setUp() {
        reservationRepository = mock(ReservationRepository.class);
        facilityRepository = mock(FacilityRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-09-27T01:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new ReservationService(reservationRepository, facilityRepository, clock);
        facility = new Facility("강남구민체육관", "배드민턴장", "11680", "서울특별시 강남구",
                "서울 강남구 체육관로 1", "02-0000-0000", 1L, "정상 운영");
        facility.enrichPublicData(null, null, null, null, null, 5000,
                "1인 1시간 5,000원", null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(facility, "id", 10L);
        when(facilityRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(facility));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void createsOneHourReservationForAvailableSlot() {
        var response = service.create(20L, "11680",
                new ReservationCreateRequest(10L, LocalDate.of(2026, 10, 1), LocalTime.of(19, 0), 2));

        assertEquals(LocalTime.of(20, 0), response.endTime());
        assertEquals(5000, response.pricePerPerson());
        assertEquals(10000, response.totalFee());
        assertEquals(ReservationStatus.CONFIRMED, response.status());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void rejectsAlreadyReservedSlot() {
        when(reservationRepository.sumParticipantsForSlot(anyLong(), any(),
                eq(ReservationStatus.CONFIRMED), any(), any())).thenReturn(20);

        assertThrows(ResponseStatusException.class, () -> service.create(20L, "11680",
                new ReservationCreateRequest(10L, LocalDate.of(2026, 10, 1), LocalTime.of(19, 0), 2)));
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void allowsReservationWithinRemainingCapacity() {
        when(reservationRepository.sumParticipantsForSlot(anyLong(), any(),
                eq(ReservationStatus.CONFIRMED), any(), any())).thenReturn(18);

        var response = service.create(20L, "11680",
                new ReservationCreateRequest(10L, LocalDate.of(2026, 10, 1), LocalTime.of(19, 0), 2));

        assertEquals(2, response.participantCount());
        verify(reservationRepository).save(any());
    }

    @Test
    void rejectsFacilityOutsideUsersRegion() {
        assertThrows(ResponseStatusException.class, () -> service.create(20L, "11710",
                new ReservationCreateRequest(10L, LocalDate.of(2026, 10, 1), LocalTime.of(19, 0), 2)));
    }

    @Test
    void userCannotCancelAnotherUsersReservation() {
        Reservation reservation = new Reservation(99L, facility, LocalDate.of(2026, 10, 1),
                LocalTime.of(19, 0), LocalTime.of(20, 0), 2, 5000);
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(reservation));

        assertThrows(ResponseStatusException.class, () -> service.cancel(20L, 1L));
    }

    @Test
    void returnsFigmaReservationDateAndTimeOptions() {
        when(facilityRepository.findById(10L)).thenReturn(Optional.of(facility));
        when(reservationRepository.findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
                10L, LocalDate.of(2026, 10, 1), ReservationStatus.CONFIRMED)).thenReturn(java.util.List.of());

        var response = service.options("11680", 10L, LocalDate.of(2026, 10, 1));

        assertEquals(5, response.dates().size());
        assertEquals(4, response.timeSlots().size());
        assertEquals("AVAILABLE", response.timeSlots().get(0).status());
        assertEquals(20, response.timeSlots().get(0).remainingCapacity());
        assertEquals(5000, response.pricePerPerson());
    }

    @Test
    void unknownPublicFeeUsesSingleMvpFallbackAcrossOptionsAndReservation() {
        Facility unknownFeeFacility = new Facility("가로공원 (1)_남단", "체육시설", "11680",
                "서울특별시 강남구", "서울 강남구", null, 1L, "정상 운영");
        ReflectionTestUtils.setField(unknownFeeFacility, "id", 11L);
        when(facilityRepository.findById(11L)).thenReturn(Optional.of(unknownFeeFacility));
        when(facilityRepository.findByIdForUpdate(11L)).thenReturn(Optional.of(unknownFeeFacility));
        when(reservationRepository.findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
                11L, LocalDate.of(2026, 10, 1), ReservationStatus.CONFIRMED)).thenReturn(java.util.List.of());

        var options = service.options("11680", 11L, LocalDate.of(2026, 10, 1));

        assertEquals(3333, options.pricePerPerson());
        assertTrue(options.timeSlots().stream().allMatch(slot -> slot.pricePerPerson() == 3333));

        var reservation = service.create(20L, "11680", new ReservationCreateRequest(
                11L, LocalDate.of(2026, 10, 1), LocalTime.of(19, 0), 2));
        assertEquals(3333, reservation.pricePerPerson());
        assertEquals(6666, reservation.totalFee());
    }
}
