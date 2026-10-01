package com.cheche.facility.service;

import com.cheche.facility.domain.*;
import com.cheche.facility.dto.*;
import com.cheche.facility.repository.FacilityRepository;
import com.cheche.facility.repository.ReservationRepository;
import java.time.*;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
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
        verifySchedule(facility, request.reservationDate(), request.startTime());

        LocalTime endTime = request.startTime().plusHours(1);
        int capacity = capacity(facility);
        int reserved = reservationRepository.sumParticipantsForSlot(facility.getId(), request.reservationDate(),
                ReservationStatus.CONFIRMED, endTime, request.startTime());
        if (reserved + request.participantCount() > capacity) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "선택한 시간의 잔여 인원은 " + Math.max(0, capacity - reserved) + "명입니다.");
        }
        int price = resolvedPrice(facility);
        Reservation reservation = new Reservation(userId, facility, request.reservationDate(),
                request.startTime(), endTime, request.participantCount(), price);
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
        List<Reservation> reservations = reservationRepository
                .findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
                        facilityId, date, ReservationStatus.CONFIRMED);
        int capacity = capacity(facility);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalTime opening = openingTime(facility, date);
        LocalTime lastStart = closingTime(facility, date).minusHours(1);
        List<LocalTime> available = java.util.stream.IntStream.rangeClosed(opening.getHour(), lastStart.getHour())
                .mapToObj(hour -> LocalTime.of(hour, 0))
                .filter(time -> reservedParticipants(reservations, time) < capacity)
                .filter(time -> LocalDateTime.of(date, time).isAfter(now))
                .toList();
        return new AvailabilityResponse(facilityId, date, available);
    }

    @Transactional(readOnly = true)
    public ReservationOptionsResponse options(String regionCode, Long facilityId, LocalDate selectedDate) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "체육시설을 찾을 수 없습니다."));
        verifyFacility(facility, regionCode);
        LocalDate today = LocalDate.now(clock);
        LocalDate date = selectedDate == null ? today.plusDays(1) : selectedDate;
        if (date.isBefore(today) || date.isAfter(today.plusDays(RESERVATION_DAYS_LIMIT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약일은 오늘부터 90일 이내로 선택해 주세요.");
        }

        List<ReservationDateOption> dates = java.util.stream.IntStream.range(0, 5)
                .mapToObj(today::plusDays)
                .map(value -> new ReservationDateOption(value, dayOfWeek(value),
                        value.equals(today) ? "오늘" : value.equals(today.plusDays(1)) ? "내일" : dayOfWeek(value),
                        hasFutureSlot(value)))
                .toList();
        List<Reservation> reservations = reservationRepository
                .findAllByFacilityIdAndReservationDateAndStatusOrderByStartTimeAsc(
                        facilityId, date, ReservationStatus.CONFIRMED);
        LocalDateTime now = LocalDateTime.now(clock);
        LocalTime opening = optionOpeningTime(facility, date);
        LocalTime lastStart = optionClosingTime(facility, date).minusHours(1);
        int price = FacilityPricingPolicy.resolve(facility);
        int capacity = capacity(facility);
        List<ReservationTimeSlot> slots = java.util.stream.IntStream.rangeClosed(opening.getHour(), lastStart.getHour())
                .mapToObj(hour -> LocalTime.of(hour, 0))
                .map(start -> {
                    int reserved = reservedParticipants(reservations, start);
                    int remaining = Math.max(0, capacity - reserved);
                    String status = !LocalDateTime.of(date, start).isAfter(now) ? "CLOSED"
                            : remaining == 0 ? "RESERVED" : "AVAILABLE";
                    String label = switch (status) {
                        case "AVAILABLE" -> "예약 가능";
                        case "RESERVED" -> "예약 완료";
                        default -> "마감";
                    };
                    return new ReservationTimeSlot(start, start.plusHours(1), status, label, price,
                            capacity, reserved, remaining);
                }).toList();
        return new ReservationOptionsResponse(facilityId, facility.getName(), facility.getType(), date,
                price, 1, capacity, dates, slots);
    }

    @Transactional(readOnly = true)
    public ReservationCheckoutResponse checkout(String regionCode, Long facilityId) {
        Facility facility = facilityRepository.findById(facilityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "체육시설을 찾을 수 없습니다."));
        verifyFacility(facility, regionCode);
        int price = FacilityPricingPolicy.resolve(facility);
        boolean external = facility.getSourceUrl() != null && !facility.getSourceUrl().isBlank();
        return new ReservationCheckoutResponse(facilityId, facility.getName(), price,
                external ? "CHECHE_OR_EXTERNAL" : "CHECHE", external, facility.getSourceUrl(),
                price > 0, false,
                external
                        ? "CheChe 내부 예약 또는 제공기관 예약 페이지를 이용할 수 있습니다."
                        : "CheChe 내부 예약을 이용해 주세요. 실결제는 PG 연동 후 활성화됩니다.");
    }

    private boolean hasFutureSlot(LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock);
        return java.util.stream.IntStream.rangeClosed(18, 21)
                .mapToObj(hour -> LocalDateTime.of(date, LocalTime.of(hour, 0)))
                .anyMatch(value -> value.isAfter(now));
    }

    private int capacity(Facility facility) {
        return facility.getCapacity() == null || facility.getCapacity() < 1 ? 20 : facility.getCapacity();
    }

    private int resolvedPrice(Facility facility) {
        return FacilityPricingPolicy.resolve(facility);
    }

    private int reservedParticipants(List<Reservation> reservations, LocalTime start) {
        LocalTime end = start.plusHours(1);
        return reservations.stream()
                .filter(reservation -> reservation.getStartTime().isBefore(end)
                        && reservation.getEndTime().isAfter(start))
                .mapToInt(Reservation::getParticipantCount).sum();
    }

    private LocalTime openingTime(Facility facility, LocalDate date) {
        return parseTime(isWeekend(date) ? facility.getWeekendOpeningTime() : facility.getWeekdayOpeningTime(), OPEN_TIME);
    }

    private LocalTime optionOpeningTime(Facility facility, LocalDate date) {
        String value = isWeekend(date) ? facility.getWeekendOpeningTime() : facility.getWeekdayOpeningTime();
        return parseTime(value, LocalTime.of(18, 0));
    }

    private LocalTime optionClosingTime(Facility facility, LocalDate date) {
        String value = isWeekend(date) ? facility.getWeekendClosingTime() : facility.getWeekdayClosingTime();
        return parseTime(value, LocalTime.of(22, 0));
    }

    private LocalTime closingTime(Facility facility, LocalDate date) {
        return parseTime(isWeekend(date) ? facility.getWeekendClosingTime() : facility.getWeekdayClosingTime(),
                LAST_START_TIME.plusHours(1));
    }

    private LocalTime parseTime(String value, LocalTime fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalTime.parse(value);
        } catch (java.time.format.DateTimeParseException exception) {
            return fallback;
        }
    }

    private boolean isWeekend(LocalDate date) {
        return date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    private String dayOfWeek(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            case SUNDAY -> "일";
        };
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

    private void verifySchedule(Facility facility, LocalDate date, LocalTime startTime) {
        LocalDate today = LocalDate.now(clock);
        if (date.isAfter(today.plusDays(RESERVATION_DAYS_LIMIT))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "예약일은 오늘부터 90일 이내로 선택해 주세요.");
        }
        LocalTime opening = openingTime(facility, date);
        LocalTime lastStart = closingTime(facility, date).minusHours(1);
        if (startTime.getMinute() != 0 || startTime.isBefore(opening) || startTime.isAfter(lastStart)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "시설 운영시간 안에서 정각 단위로 예약해 주세요.");
        }
        if (!LocalDateTime.of(date, startTime).isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 시각 이후의 일정만 예약할 수 있습니다.");
        }
    }
}
