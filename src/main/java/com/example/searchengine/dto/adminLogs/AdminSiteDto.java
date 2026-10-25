package com.example.searchengine.dto.adminLogs;

import java.time.LocalDateTime;

public record AdminSiteDto(
        Long id,
        String url,
        String name,
        String status,
        LocalDateTime statusTime,
        long pagesCount,
        String lastError
) {}
