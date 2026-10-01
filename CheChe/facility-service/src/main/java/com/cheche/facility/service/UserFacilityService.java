package com.cheche.facility.service;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityFavorite;
import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.*;
import com.cheche.facility.repository.FacilityRepository;
import com.cheche.facility.repository.FacilityFavoriteRepository;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserFacilityService {
    private static final int HOME_RECOMMENDATION_LIMIT = 6;
    private static final List<String> QUICK_SPORTS = List.of("축구", "배드민턴", "수영", "농구");
    private static final List<String> SPORTS = List.of("배드민턴", "수영", "축구", "풋살", "농구", "테니스", "탁구", "헬스");
    private static final List<String> DISTRICTS = List.of("종로구", "중구", "용산구", "성동구", "광진구", "동대문구", "중랑구", "성북구", "강북구", "도봉구", "노원구", "은평구", "서대문구", "마포구", "양천구", "강서구", "구로구", "금천구", "영등포구", "동작구", "관악구", "서초구", "강남구", "송파구", "강동구");
    private static final Pattern HOUR_PATTERN = Pattern.compile("(\\d{1,2})\\s*시");
    private static final Set<String> STOP_WORDS = Set.of(
            "시설", "추천", "검색", "근처", "주변", "있는", "가능한", "곳", "장소",
            "찾아줘", "보여줘", "알려줘", "이용", "하고", "싶어", "할수있는");

    private final FacilityRepository repository;
    private final SeoulPublicFacilityClient seoulPublicFacilityClient;
    private final KspoSportsValueClient kspoSportsValueClient;
    private final FacilityFavoriteRepository favoriteRepository;

    public UserFacilityService(FacilityRepository repository,
                               SeoulPublicFacilityClient seoulPublicFacilityClient,
                               KspoSportsValueClient kspoSportsValueClient,
                               FacilityFavoriteRepository favoriteRepository) {
        this.repository = repository;
        this.seoulPublicFacilityClient = seoulPublicFacilityClient;
        this.kspoSportsValueClient = kspoSportsValueClient;
        this.favoriteRepository = favoriteRepository;
    }

    @Transactional(readOnly = true)
    public UserHomeResponse home(Long userId, String regionCode) {
        return home(userId, regionCode, null, null);
    }

    @Transactional(readOnly = true)
    public UserHomeResponse home(Long userId, String regionCode, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        List<UserFacilityCard> recommendations = markFavorites(userId,
                regionalFacilityCards(regionCode, latitude, longitude)).stream()
                .sorted(distanceComparator())
                .limit(HOME_RECOMMENDATION_LIMIT)
                .toList();
        List<UserFacilityCard> kspoFacilities = markFavorites(userId, kspoSportsValueClient.findAll()).stream()
                .map(UserFacilityCard::withDefaultImage).limit(6).toList();
        return new UserHomeResponse("내 주변 체육시설", "원하는 운동이나 지역을 자연어로 검색해 보세요.",
                blankToNull(regionCode), "오늘 저녁 7시에 이용 가능한 배드민턴장 찾아줘",
                QUICK_SPORTS, recommendations, kspoFacilities);
    }

    @Transactional(readOnly = true)
    public NaturalLanguageSearchResponse search(Long userId, NaturalLanguageSearchRequest request, String regionCode) {
        return search(userId, request, regionCode, null, null);
    }

    @Transactional(readOnly = true)
    public NaturalLanguageSearchResponse search(Long userId, NaturalLanguageSearchRequest request, String regionCode,
                                                Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        List<String> keywords = keywords(request.query());
        List<UserFacilityCard> matches = availableFacilityCards(regionCode, latitude, longitude).stream()
                .map(facility -> new ScoredFacility(facility, score(facility, keywords)))
                .filter(candidate -> keywords.isEmpty() || candidate.score() > 0)
                .sorted(Comparator.comparingInt(ScoredFacility::score).reversed()
                        .thenComparing(candidate -> candidate.facility().name()))
                .map(ScoredFacility::facility)
                .toList();
        matches = markFavorites(userId, matches);

        boolean empty = matches.isEmpty();
        AiSearchConditions conditions = conditions(request.query());
        UserFacilityCard recommended = empty ? null : matches.get(0);
        return new NaturalLanguageSearchResponse(
                request.query(), matches.size(), empty,
                empty ? "조건에 맞는 체육시설을 찾지 못했어요." : matches.size() + "개의 체육시설을 찾았어요.",
                empty ? "조건을 조금 바꾸어 다시 검색해 보세요."
                        : "조건에 맞는 시설을 찾았어요. 운영 상태와 예약 가능 시간을 확인해 주세요.",
                conditions, recommended,
                empty ? List.of("지역명을 줄여서 검색해 보세요.", "운동 종목만 입력해 보세요.", "다른 지역이나 종목을 검색해 보세요.") : List.of(),
                matches);
    }

    @Transactional(readOnly = true)
    public UserFacilityDetailResponse detail(Long userId, Long id, String regionCode) {
        return detail(userId, id, regionCode, null, null);
    }

    @Transactional(readOnly = true)
    public UserFacilityDetailResponse detail(Long userId, Long id, String regionCode,
                                             Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        Facility facility = find(id);
        verifyRegion(facility, regionCode);
        return UserFacilityDetailResponse.from(facility, favoriteRepository.existsByUserIdAndFacilityId(userId, id),
                distance(latitude, longitude, facility.getLatitude(), facility.getLongitude()));
    }

    @Transactional
    public FavoriteResponse addFavorite(Long userId, Long id, String regionCode) {
        Facility facility = find(id);
        verifyRegion(facility, regionCode);
        if (!favoriteRepository.existsByUserIdAndFacilityId(userId, id)) {
            favoriteRepository.save(new FacilityFavorite(userId, id));
        }
        return new FavoriteResponse(id, true);
    }

    @Transactional
    public FavoriteResponse removeFavorite(Long userId, Long id, String regionCode) {
        Facility facility = find(id);
        verifyRegion(facility, regionCode);
        favoriteRepository.deleteByUserIdAndFacilityId(userId, id);
        return new FavoriteResponse(id, false);
    }

    @Transactional(readOnly = true)
    public List<UserFacilityCard> favorites(Long userId, String regionCode) {
        return favorites(userId, regionCode, null, null);
    }

    @Transactional(readOnly = true)
    public List<UserFacilityCard> favorites(Long userId, String regionCode, Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        Set<Long> ids = favoriteIds(userId);
        return operatingFacilities(regionCode).stream()
                .filter(facility -> ids.contains(facility.getId()))
                .map(facility -> UserFacilityCard.from(facility).withDistance(distance(latitude, longitude,
                        facility.getLatitude(), facility.getLongitude())))
                .map(card -> card.withFavorite(true)).sorted(distanceComparator()).toList();
    }

    @Transactional(readOnly = true)
    public UsageGuideResponse usageGuide(Long id, String regionCode) {
        Facility facility = find(id);
        verifyRegion(facility, regionCode);
        boolean reservable = facility.getStatus() == FacilityStatus.OPERATING;
        String channel = facility.getPhone() == null || facility.getPhone().isBlank()
                ? "시설 현장 문의" : "시설 전화 문의";
        List<String> steps = reservable
                ? List.of("운영 여부와 이용 가능 시간을 확인해 주세요.", "시설에 예약 가능 여부를 문의해 주세요.", "안내받은 방법으로 예약을 완료해 주세요.")
                : List.of("현재 시설 상태를 확인해 주세요.", "운영 재개 여부는 시설에 문의해 주세요.");
        return new UsageGuideResponse(facility.getId(), facility.getName(), reservable, channel,
                facility.getPhone(), facility.getPublicNotice(), steps);
    }

    private List<Facility> operatingFacilities(String regionCode) {
        String normalizedRegion = blankToNull(regionCode);
        return normalizedRegion == null
                ? repository.findAllByStatusOrderByNameAsc(FacilityStatus.OPERATING)
                : repository.findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus.OPERATING, normalizedRegion);
    }

    private List<UserFacilityCard> availableFacilityCards(String regionCode, Double latitude, Double longitude) {
        List<UserFacilityCard> combined = new ArrayList<>(regionalFacilityCards(regionCode, latitude, longitude));
        combined.addAll(kspoSportsValueClient.findAll());
        return deduplicate(combined);
    }

    private List<UserFacilityCard> regionalFacilityCards(String regionCode, Double latitude, Double longitude) {
        List<UserFacilityCard> combined = new ArrayList<>();
        operatingFacilities(regionCode).stream()
                .map(facility -> UserFacilityCard.from(facility).withDistance(distance(latitude, longitude,
                        facility.getLatitude(), facility.getLongitude())))
                .forEach(combined::add);
        combined.addAll(seoulPublicFacilityClient.findByRegion(regionCode));
        return deduplicate(combined).stream().map(UserFacilityCard::withDefaultImage).toList();
    }

    private List<UserFacilityCard> deduplicate(List<UserFacilityCard> combined) {
        Map<String, UserFacilityCard> unique = new LinkedHashMap<>();
        combined.forEach(card -> unique.putIfAbsent(
                normalize(card.regionName()) + '|' + normalize(card.name()) + '|' + normalize(card.type()), card));
        return new ArrayList<>(unique.values());
    }

    private List<UserFacilityCard> markFavorites(Long userId, List<UserFacilityCard> cards) {
        Set<Long> ids = favoriteIds(userId);
        return cards.stream().map(card -> card.id() != null && ids.contains(card.id())
                ? card.withFavorite(true) : card).map(UserFacilityCard::withDefaultImage).toList();
    }

    private Set<Long> favoriteIds(Long userId) {
        if (userId == null) return Set.of();
        Set<Long> ids = new HashSet<>();
        favoriteRepository.findAllByUserId(userId).forEach(value -> ids.add(value.getFacilityId()));
        return ids;
    }

    private AiSearchConditions conditions(String query) {
        String region = DISTRICTS.stream().filter(query::contains).findFirst().orElse(null);
        String sport = SPORTS.stream().filter(query::contains).findFirst().orElse(null);
        Matcher matcher = HOUR_PATTERN.matcher(query);
        String time = matcher.find() ? matcher.group(1) + "시 이후"
                : query.contains("저녁") ? "저녁" : query.contains("오전") ? "오전" : null;
        return new AiSearchConditions(region, sport, time, query.contains("예약") || query.contains("가능"));
    }

    private Facility find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "체육시설을 찾을 수 없습니다."));
    }

    private void verifyRegion(Facility facility, String regionCode) {
        if (!facility.getRegionCode().equals(regionCode)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "설정한 지역의 체육시설만 이용할 수 있습니다.");
        }
    }

    private List<String> keywords(String query) {
        return Arrays.stream(query.toLowerCase(Locale.KOREAN).trim().split("[^\\p{L}\\p{N}]+"))
                .map(this::removeParticle)
                .filter(token -> token.length() >= 2 && !STOP_WORDS.contains(token))
                .distinct()
                .toList();
    }

    private String removeParticle(String token) {
        return token.replaceFirst("(에서|으로|에게|부터|까지|하고|이며|이고|은|는|이|가|을|를|와|과|의|에)$", "")
                .replaceFirst("할$", "");
    }

    private int score(UserFacilityCard facility, List<String> keywords) {
        String name = normalize(facility.name());
        String type = normalize(facility.type());
        String region = normalize(facility.regionName());
        String address = normalize(facility.address());
        return keywords.stream().mapToInt(keyword -> {
            String normalized = normalize(keyword);
            if (name.contains(normalized)) return 5;
            if (type.contains(normalized)) return 4;
            if (region.contains(normalized) || address.contains(normalized)) return 3;
            return 0;
        }).sum();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.KOREAN).replaceAll("\\s+", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "위도와 경도를 함께 입력해 주세요.");
        }
        if (latitude != null && (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "위치 좌표 범위가 올바르지 않습니다.");
        }
    }

    private Double distance(Double userLatitude, Double userLongitude,
                            Double facilityLatitude, Double facilityLongitude) {
        if (userLatitude == null || userLongitude == null || facilityLatitude == null || facilityLongitude == null) {
            return null;
        }
        double latDistance = Math.toRadians(facilityLatitude - userLatitude);
        double lonDistance = Math.toRadians(facilityLongitude - userLongitude);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLatitude)) * Math.cos(Math.toRadians(facilityLatitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double kilometers = 6371.0088 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(kilometers * 10.0) / 10.0;
    }

    private Comparator<UserFacilityCard> distanceComparator() {
        return Comparator.comparing(UserFacilityCard::distanceKm,
                Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(UserFacilityCard::name);
    }

    private record ScoredFacility(UserFacilityCard facility, int score) {}
}
