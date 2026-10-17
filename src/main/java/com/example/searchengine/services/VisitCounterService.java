package com.example.searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class VisitCounterService {

    private static final Logger logger = LoggerFactory.getLogger(VisitCounterService.class);

    private final AtomicLong totalVisits = new AtomicLong(0);

    public void increment() {
        long count = totalVisits.incrementAndGet();
        logger.debug("👋 Посещение главной: всего {}", count);
    }

    public long getTotalVisits() {
        return totalVisits.get();
    }
}
