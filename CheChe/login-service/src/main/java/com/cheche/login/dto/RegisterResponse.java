package com.cheche.login.dto;

import java.time.LocalDateTime;

public record RegisterResponse(Long userId, String username, LocalDateTime createdAt) {}
