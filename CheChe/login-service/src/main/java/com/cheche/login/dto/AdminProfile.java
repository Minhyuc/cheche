package com.cheche.login.dto;

public record AdminProfile(
        Long userId,
        String role,
        String status,
        String regionCode,
        String regionName,
        boolean initialSetupRequired
) {}
