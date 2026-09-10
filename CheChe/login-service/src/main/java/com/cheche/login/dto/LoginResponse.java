package com.cheche.login.dto;

public record LoginResponse(
        Long userId,
        String username,
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        String role,
        String regionCode,
        String regionName,
        boolean initialSetupRequired
) {}
