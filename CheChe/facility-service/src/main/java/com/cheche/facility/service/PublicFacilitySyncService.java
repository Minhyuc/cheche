package com.cheche.facility.service;

import com.cheche.facility.domain.AdminRole;
import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.PublicFacilitySyncResponse;
import com.cheche.facility.integration.KspoFacilityClient;
import com.cheche.facility.integration.KspoFacilityItem;
import com.cheche.facility.integration.PublicOpenFacilityClient;
import com.cheche.facility.integration.PublicOpenFacilityItem;
import com.cheche.facility.repository.FacilityRepository;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

@Service
public class PublicFacilitySyncService {
    static final String SOURCE = "KSPO_NATIONAL_FACILITY";
    static final String OPEN_SOURCE = "PUBLIC_OPEN_FACILITY";
    private static final String SEOUL = "서울특별시";
    private static final Map<String, String> SEOUL_DISTRICTS = Map.ofEntries(
            Map.entry("11110", "종로구"), Map.entry("11140", "중구"),
            Map.entry("11170", "용산구"), Map.entry("11200", "성동구"),
            Map.entry("11215", "광진구"), Map.entry("11230", "동대문구"),
            Map.entry("11260", "중랑구"), Map.entry("11290", "성북구"),
            Map.entry("11305", "강북구"), Map.entry("11320", "도봉구"),
            Map.entry("11350", "노원구"), Map.entry("11380", "은평구"),
            Map.entry("11410", "서대문구"), Map.entry("11440", "마포구"),
            Map.entry("11470", "양천구"), Map.entry("11500", "강서구"),
            Map.entry("11530", "구로구"), Map.entry("11545", "금천구"),
            Map.entry("11560", "영등포구"), Map.entry("11590", "동작구"),
            Map.entry("11620", "관악구"), Map.entry("11650", "서초구"),
            Map.entry("11680", "강남구"), Map.entry("11710", "송파구"),
            Map.entry("11740", "강동구"));
    private static final Map<String, String> DISTRICT_CODES = SEOUL_DISTRICTS.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Map.Entry::getValue, Map.Entry::getKey));

    private final FacilityRepository repository;
    private final KspoFacilityClient client;
    private final PublicOpenFacilityClient publicOpenFacilityClient;

    @Autowired
    public PublicFacilitySyncService(FacilityRepository repository, KspoFacilityClient client,
                                     PublicOpenFacilityClient publicOpenFacilityClient) {
        this.repository = repository;
        this.client = client;
        this.publicOpenFacilityClient = publicOpenFacilityClient;
    }

    PublicFacilitySyncService(FacilityRepository repository, KspoFacilityClient client) {
        this(repository, client, () -> new PublicOpenFacilityClient.FetchResult(0, List.of()));
    }

    @Transactional
    public PublicFacilitySyncResponse sync(Long userId, AdminRole role, String adminRegionCode) {
        SyncTarget target = resolveTarget(role, adminRegionCode);

        KspoFacilityClient.FetchResult fetched;
        try {
            fetched = client.fetchPublicFacilities(SEOUL, target.district());
        } catch (ResponseStatusException exception) {
            if (exception.getStatusCode() != HttpStatus.SERVICE_UNAVAILABLE) throw exception;
            fetched = new KspoFacilityClient.FetchResult(0, List.of());
        }
        List<KspoFacilityItem> matches = fetched.items().stream()
                .filter(item -> isTargetRegion(item, target)).toList();
        Map<String, Facility> existing = new HashMap<>();
        List<Facility> existingFacilities = target.district() == null
                ? repository.findAllBySource(SOURCE)
                : repository.findAllBySourceAndRegionCode(SOURCE, target.regionCode());
        existingFacilities
                .forEach(facility -> existing.put(facility.getExternalId(), facility));

        int created = 0;
        int updated = 0;
        for (KspoFacilityItem item : matches) {
            String externalId = externalId(item);
            String regionCode = DISTRICT_CODES.get(clean(item.district()));
            String regionName = SEOUL + " " + SEOUL_DISTRICTS.get(regionCode);
            Facility facility = existing.get(externalId);
            if (facility == null) {
                repository.save(Facility.fromPublicData(clean(item.name()), facilityType(item),
                        regionCode, regionName, address(item), phone(item), userId,
                        initialStatus(item), SOURCE, externalId, clean(item.homepage())));
                created++;
            } else {
                facility.updatePublicData(clean(item.name()), facilityType(item),
                        address(item), phone(item), clean(item.homepage()));
                updated++;
            }
        }
        PublicOpenFacilityClient.FetchResult openData = publicOpenFacilityClient.fetchAll();
        List<PublicOpenFacilityItem> openMatches = openData.items().stream()
                .filter(this::isSportsFacility)
                .filter(item -> districtCode(item) != null)
                .filter(item -> target.district() == null
                        || target.regionCode().equals(districtCode(item)))
                .toList();

        List<Facility> allInTarget = target.district() == null
                ? repository.findAll()
                : repository.findAllByRegionCodeOrderByNameAsc(target.regionCode());
        Map<String, Facility> byIdentity = new HashMap<>();
        Map<String, Facility> byName = new HashMap<>();
        allInTarget.forEach(facility -> {
            byIdentity.put(identity(facility.getName(), facility.getAddress()), facility);
            byName.putIfAbsent(normalize(facility.getName()), facility);
        });

        for (PublicOpenFacilityItem item : openMatches) {
            String regionCode = districtCode(item);
            String address = publicAddress(item);
            String name = clean(item.name());
            Facility facility = byIdentity.get(identity(name, address));
            if (facility == null) facility = byName.get(normalize(name));
            if (facility == null) {
                String externalId = UUID.nameUUIDFromBytes((name + '|' + address + '|'
                        + clean(item.institutionName())).getBytes(StandardCharsets.UTF_8)).toString();
                facility = Facility.fromPublicData(name, publicFacilityType(item), regionCode,
                        SEOUL + " " + SEOUL_DISTRICTS.get(regionCode), address, clean(item.phone()), userId,
                        FacilityStatus.OPERATING, OPEN_SOURCE, externalId, clean(item.homepageUrl()));
                enrich(facility, item);
                repository.save(facility);
                byIdentity.put(identity(name, address), facility);
                byName.putIfAbsent(normalize(name), facility);
                created++;
            } else {
                facility.updatePublicData(name, publicFacilityType(item), address, clean(item.phone()),
                        clean(item.homepageUrl()).isBlank() ? facility.getSourceUrl() : clean(item.homepageUrl()));
                enrich(facility, item);
                updated++;
            }
        }
        return new PublicFacilitySyncResponse("국민체육진흥공단·공공데이터포털", target.regionCode(), target.regionName(),
                fetched.items().size() + openData.totalCount(), matches.size() + openMatches.size(), created, updated);
    }

    private void enrich(Facility facility, PublicOpenFacilityItem item) {
        facility.enrichPublicData(clean(item.imageUrl()), clean(item.weekdayOpeningTime()),
                clean(item.weekdayClosingTime()), clean(item.weekendOpeningTime()),
                clean(item.weekendClosingTime()), fee(item), clean(item.fee()), integer(item.capacity()),
                clean(item.amenities()), clean(item.applicationMethod()), clean(item.closedDays()),
                decimal(item.latitude()), decimal(item.longitude()), clean(item.homepageUrl()));
    }

    private boolean isSportsFacility(PublicOpenFacilityItem item) {
        String text = normalize(clean(item.facilityType()) + " " + clean(item.name()) + " " + clean(item.locationName()));
        return List.of("체육", "운동", "축구", "풋살", "농구", "배드민턴", "테니스", "수영",
                "골프", "탁구", "헬스", "야구", "족구", "체력").stream().anyMatch(text::contains);
    }

    private String districtCode(PublicOpenFacilityItem item) {
        String address = publicAddress(item);
        return DISTRICT_CODES.entrySet().stream()
                .filter(entry -> address.contains(entry.getKey()))
                .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    private String publicAddress(PublicOpenFacilityItem item) {
        String road = clean(item.roadAddress());
        return road.isBlank() ? clean(item.lotAddress()) : road;
    }

    private String publicFacilityType(PublicOpenFacilityItem item) {
        String type = clean(item.facilityType());
        return type.isBlank() ? "공공체육시설" : type;
    }

    private Integer fee(PublicOpenFacilityItem item) {
        if (clean(item.paid()).contains("무료")) return 0;
        Matcher matcher = Pattern.compile("[0-9][0-9,]*").matcher(clean(item.fee()));
        if (!matcher.find()) return null;
        try { return Integer.parseInt(matcher.group().replace(",", "")); }
        catch (NumberFormatException exception) { return null; }
    }

    private Integer integer(String value) {
        Matcher matcher = Pattern.compile("[0-9]+").matcher(clean(value));
        if (!matcher.find()) return null;
        try { return Integer.parseInt(matcher.group()); }
        catch (NumberFormatException exception) { return null; }
    }

    private Double decimal(String value) {
        try { return clean(value).isBlank() ? null : Double.parseDouble(clean(value)); }
        catch (NumberFormatException exception) { return null; }
    }

    private String identity(String name, String address) {
        return normalize(name) + '|' + normalize(address);
    }

    private String normalize(String value) {
        return clean(value).toLowerCase(java.util.Locale.KOREAN).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private SyncTarget resolveTarget(AdminRole role, String adminRegionCode) {
        if (role == AdminRole.SUPER_USER) return new SyncTarget("11", SEOUL, null);
        String district = SEOUL_DISTRICTS.get(adminRegionCode);
        if (district == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "서울특별시 25개 자치구 관리자만 공공데이터를 동기화할 수 있습니다.");
        }
        return new SyncTarget(adminRegionCode, SEOUL + " " + district, district);
    }

    private boolean isTargetRegion(KspoFacilityItem item, SyncTarget target) {
        String district = clean(item.district());
        return SEOUL.equals(clean(item.province())) && DISTRICT_CODES.containsKey(district)
                && (target.district() == null || target.district().equals(district));
    }

    private String externalId(KspoFacilityItem item) {
        String code = clean(item.facilityCode());
        if (code.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "공공데이터 시설코드가 없습니다.");
        return code;
    }

    private String facilityType(KspoFacilityItem item) {
        String type = clean(item.facilityType());
        return !type.isBlank() ? type : !clean(item.businessType()).isBlank()
                ? clean(item.businessType()) : "공공체육시설";
    }

    private String address(KspoFacilityItem item) {
        String road = join(item.roadAddress(), item.roadAddressDetail());
        return !road.isBlank() ? road : join(item.address(), item.addressDetail());
    }

    private String phone(KspoFacilityItem item) {
        String phone = clean(item.phone());
        return !phone.isBlank() ? phone : clean(item.managerPhone());
    }

    private FacilityStatus initialStatus(KspoFacilityItem item) {
        String status = clean(item.status());
        return status.contains("폐업") || status.contains("휴업") ? FacilityStatus.CLOSED : FacilityStatus.OPERATING;
    }

    private String join(String first, String second) {
        return (clean(first) + " " + clean(second)).trim();
    }

    private String clean(String value) {
        if (value == null) return "";
        String result = value;
        for (int i = 0; i < 2; i++) result = HtmlUtils.htmlUnescape(result);
        return result.replace("&#40;", "(").replace("&#41;", ")")
                .replace("&&#35;40;", "(").replace("&&#35;41;", ")").trim();
    }

    private record SyncTarget(String regionCode, String regionName, String district) {}
}
