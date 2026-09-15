package com.cheche.login.dto;

public record UserLoginResponse(
        Long userId,
        String username,
        String accountType,
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String regionCode,
        String regionName,
        boolean initialSetupRequired,
        String message
) {}
