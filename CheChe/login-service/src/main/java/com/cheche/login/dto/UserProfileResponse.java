package com.cheche.login.dto;

import com.cheche.login.domain.User;

public record UserProfileResponse(
        Long userId,
        String username,
        String regionCode,
        String regionName,
        boolean initialSetupRequired
) {
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getRegionCode(),
                user.getRegionName(), user.getRegionCode() == null);
    }
}
