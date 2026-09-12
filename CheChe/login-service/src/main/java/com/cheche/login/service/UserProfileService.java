package com.cheche.login.service;

import com.cheche.login.domain.AccountType;
import com.cheche.login.domain.User;
import com.cheche.login.dto.RegionUpdateRequest;
import com.cheche.login.dto.UserProfileResponse;
import com.cheche.login.repository.UserRepository;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserProfileService {
    private static final Map<String, String> SEOUL_DISTRICTS = Map.ofEntries(
            Map.entry("11110", "서울특별시 종로구"), Map.entry("11140", "서울특별시 중구"),
            Map.entry("11170", "서울특별시 용산구"), Map.entry("11200", "서울특별시 성동구"),
            Map.entry("11215", "서울특별시 광진구"), Map.entry("11230", "서울특별시 동대문구"),
            Map.entry("11260", "서울특별시 중랑구"), Map.entry("11290", "서울특별시 성북구"),
            Map.entry("11305", "서울특별시 강북구"), Map.entry("11320", "서울특별시 도봉구"),
            Map.entry("11350", "서울특별시 노원구"), Map.entry("11380", "서울특별시 은평구"),
            Map.entry("11410", "서울특별시 서대문구"), Map.entry("11440", "서울특별시 마포구"),
            Map.entry("11470", "서울특별시 양천구"), Map.entry("11500", "서울특별시 강서구"),
            Map.entry("11530", "서울특별시 구로구"), Map.entry("11545", "서울특별시 금천구"),
            Map.entry("11560", "서울특별시 영등포구"), Map.entry("11590", "서울특별시 동작구"),
            Map.entry("11620", "서울특별시 관악구"), Map.entry("11650", "서울특별시 서초구"),
            Map.entry("11680", "서울특별시 강남구"), Map.entry("11710", "서울특별시 송파구"),
            Map.entry("11740", "서울특별시 강동구"));

    private final UserRepository repository;

    public UserProfileService(UserRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse get(Long userId) {
        return UserProfileResponse.from(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateRegion(Long userId, RegionUpdateRequest request) {
        User user = findUser(userId);
        String regionName = SEOUL_DISTRICTS.get(request.regionCode());
        if (regionName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "서울특별시 25개 자치구만 선택할 수 있습니다.");
        }
        user.updateRegion(request.regionCode(), regionName);
        return UserProfileResponse.from(user);
    }

    public Map<String, String> districts() {
        return SEOUL_DISTRICTS;
    }

    private User findUser(Long userId) {
        User user = repository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."));
        if (user.getAccountType() != AccountType.USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "사용자 계정이 아닙니다.");
        }
        return user;
    }
}
