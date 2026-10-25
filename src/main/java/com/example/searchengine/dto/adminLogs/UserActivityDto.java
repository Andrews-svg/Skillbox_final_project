package com.example.searchengine.dto.adminLogs;

import java.time.Instant;

public record UserActivityDto(
        Long id,
        Long userId,
        String username,
        String action,
        String actionDetail,
        String ipAddress,
        String userAgent,
        Instant timestamp,
        Boolean success
) {}
