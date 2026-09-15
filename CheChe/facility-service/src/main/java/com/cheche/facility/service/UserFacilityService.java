package com.cheche.facility.service;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.*;
import com.cheche.facility.repository.FacilityRepository;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserFacilityService {
    private static final int HOME_RECOMMENDATION_LIMIT = 6;
    private static final Set<String> STOP_WORDS = Set.of(
            "시설", "추천", "검색", "근처", "주변", "있는", "가능한", "곳", "장소",
            "찾아줘", "보여줘", "알려줘", "이용", "하고", "싶어", "할수있는");

    private final FacilityRepository repository;

    public UserFacilityService(FacilityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UserHomeResponse home(String regionCode) {
        List<UserFacilityCard> recommendations = operatingFacilities(regionCode).stream()
                .limit(HOME_RECOMMENDATION_LIMIT)
                .map(UserFacilityCard::from)
                .toList();
        return new UserHomeResponse("내 주변 체육시설", "원하는 운동이나 지역을 자연어로 검색해 보세요.",
                blankToNull(regionCode), recommendations);
    }

    @Transactional(readOnly = true)
    public NaturalLanguageSearchResponse search(NaturalLanguageSearchRequest request, String regionCode) {
        List<String> keywords = keywords(request.query());
        List<UserFacilityCard> matches = operatingFacilities(regionCode).stream()
                .map(facility -> new ScoredFacility(facility, score(facility, keywords)))
                .filter(candidate -> keywords.isEmpty() || candidate.score() > 0)
                .sorted(Comparator.comparingInt(ScoredFacility::score).reversed()
                        .thenComparing(candidate -> candidate.facility().getName()))
                .map(candidate -> UserFacilityCard.from(candidate.facility()))
                .toList();

        boolean empty = matches.isEmpty();
        return new NaturalLanguageSearchResponse(
                request.query(), matches.size(), empty,
                empty ? "조건에 맞는 체육시설을 찾지 못했어요." : matches.size() + "개의 체육시설을 찾았어요.",
                empty ? List.of("지역명을 줄여서 검색해 보세요.", "운동 종목만 입력해 보세요.", "다른 지역이나 종목을 검색해 보세요.") : List.of(),
                matches);
    }

    @Transactional(readOnly = true)
    public UserFacilityDetailResponse detail(Long id, String regionCode) {
        Facility facility = find(id);
        verifyRegion(facility, regionCode);
        return UserFacilityDetailResponse.from(facility);
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

    private int score(Facility facility, List<String> keywords) {
        String name = normalize(facility.getName());
        String type = normalize(facility.getType());
        String region = normalize(facility.getRegionName());
        String address = normalize(facility.getAddress());
        String notice = normalize(facility.getPublicNotice());
        return keywords.stream().mapToInt(keyword -> {
            String normalized = normalize(keyword);
            if (name.contains(normalized)) return 5;
            if (type.contains(normalized)) return 4;
            if (region.contains(normalized) || address.contains(normalized)) return 3;
            if (notice.contains(normalized)) return 1;
            return 0;
        }).sum();
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.KOREAN).replaceAll("\\s+", "");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record ScoredFacility(Facility facility, int score) {}
}
