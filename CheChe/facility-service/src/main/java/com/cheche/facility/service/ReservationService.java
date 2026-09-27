package com.cheche.facility.service;

import com.cheche.facility.domain.*;
import com.cheche.facility.dto.*;
import com.cheche.facility.repository.FacilityRepository;
import com.cheche.facility.repository.ReservationRepository;
import java.time.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReservationService {
    private static final LocalTime OPEN_TIME = LocalTime.of(6, 0);
    private static final LocalTime LAST_START_TIME = LocalTime.of(21, 0);
    private static final int RESERVATION_DAYS_LIMIT = 90;

    private final ReservationRepository reservationRepository;
    private final FacilityRepository facilityRepository;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
                              FacilityRepository facilityRepository) {
        this(reservationRepository, facilityRepository, Clock.systemDefaultZone());
    }

    ReservationService(ReservationRepository reservationRepository,
                       FacilityRepository facilityRepository, Clock clock) {
        this.reservationRepository = reservationRepository;
        this.facilityRepository = facilityRepository;
        this.clock = clock;
    }

    @Transactional
    public ReservationResponse create(Long userId, String regionCode, ReservationCreateRequest request) {
        Facility facility = facilityRepository.findByIdForUpdate(request.facilityId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "체육시설을 찾을 수 없습니다."));
        verifyFacility(facility, regionCode);
        verifySchedule(request.reservationDate(), request.startTime());

        LocalTime endTime = request.startTime().plusHours(1);
        boolean occupied = reservationRepository
                .existsByFacilityIdAndReservationDateAndStatusAndStartTimeLessThanAndEndTimeGreaterThan(
                        facility.getId(), request.reservationDate(), ReservationStatus.CONFIRMED,
                        endTime, request.startTime());
        if (occupied) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 예약된 시간입니다. 다른 시간을 선택해 주세요.");
        }

        Reservation reservation = new Reservation(userId, facility, request.reservationDate(),
                request.startTime(), endTime, request.participantCount());
        return ReservationResponse.from(reservationRepository.save(reservation));
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> list(Long userId) {
        return reservationRepository.findAllByUserIdOrderByReservationDateDescStartTimeDesc(userId)
                .stream().map(ReservationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReservationResponse get(Long userId, Long reservationId) {
        return ReservationResponse.from(findOwned(userId, reservationId));
    }

    @Transactional
    public ReservationResponse cancel(Long userId, Long reservationId) {
        Reservation reservation = findOwned(userId, reservationId);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "확정 상태의 예약만 취소할 수 있습니다.");
        }
        LocalDateTime start = LocalDateTime.of(reservation.getReservationDate(), reservation.getStartTime());
        if (!start.isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 시작된 예약은 취소할 수 없습니다.");
        }
        reservation.cancel();
        return ReservationResponse.from(reservation);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(String regionCode, Long facilityId, LocalDate date) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "체육시설을 찾을 수 없습니다."));
        verifyFacility(facility, regionCode);
        if (date.isBefore(LocalDate.now(clock)) || date.isAfter(LocalDate.now(clock).plusDays(RESERVATION_DAYS_LIMIT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약일은 오늘부터 90일 이내로 선택해 주세요.");
        }
        List<LocalTime> occupied = reservationRepository
                .findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
                        facilityId, date, ReservationStatus.CONFIRMED)
                .stream().map(Reservation::getStartTime).toList();
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalTime> available = java.util.stream.IntStream.rangeClosed(OPEN_TIME.getHour(), LAST_START_TIME.getHour())
                .mapToObj(hour -> LocalTime.of(hour, 0))
                .filter(time -> !occupied.contains(time))
                .filter(time -> LocalDateTime.of(date, time).isAfter(now))
                .toList();
        return new AvailabilityResponse(facilityId, date, available);
    }

    private Reservation findOwned(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "예약을 찾을 수 없습니다."));
        if (!reservation.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인의 예약만 확인할 수 있습니다.");
        }
        return reservation;
    }

    private void verifyFacility(Facility facility, String regionCode) {
        if (!facility.getRegionCode().equals(regionCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "설정한 지역의 체육시설만 예약할 수 있습니다.");
        }
        if (facility.getStatus() != FacilityStatus.OPERATING) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "현재 운영 중인 시설만 예약할 수 있습니다.");
        }
    }

    private void verifySchedule(LocalDate date, LocalTime startTime) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today.plusDays(RESERVATION_DAYS_LIMIT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약일은 오늘부터 90일 이내로 선택해 주세요.");
        }
        if (startTime.getMinute() != 0 || startTime.isBefore(OPEN_TIME) || startTime.isAfter(LAST_START_TIME)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약 시작 시간은 06시부터 21시까지 정각으로 선택해 주세요.");
        }
        if (!LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 시각 이후의 일정만 예약할 수 있습니다.");
        }
    }
}
