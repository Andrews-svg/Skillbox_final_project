package com.example.searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ApplicationStartupTime {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationStartupTime.class);

    private final Instant startTime;

    public ApplicationStartupTime() {
        this.startTime = Instant.now();
        logger.info("⏱ ApplicationStartupTime: старт приложения в {}", startTime);
    }

    public Instant getStartTime() {
        return startTime;
    }

    public long getUptimeSeconds() {
        return Instant.now().getEpochSecond() - startTime.getEpochSecond();
    }
}
