package com.example.searchengine.services;

import com.example.searchengine.dto.adminLogs.TopQueryDto;
import com.example.searchengine.dto.adminLogs.ZeroResultQueryDto;
import com.example.searchengine.models.statistics.SearchQueryLog;
import com.example.searchengine.repositories.statistics.SearchLogRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SearchLogService {

    private final SearchLogRepository repository;

    public SearchLogService(SearchLogRepository repository) {
        this.repository = repository;
    }


    public void logQuery(String query, int resultsCount, long responseTimeMs,
                         String userIp, String userAgent) {
        SearchQueryLog log = new SearchQueryLog();
        log.setQuery(query);
        log.setTimestamp(LocalDateTime.now());
        log.setResultsCount(resultsCount);
        log.setClicked(false);
        log.setResponseTimeMs(responseTimeMs);
        log.setUserIp(userIp);
        log.setUserAgent(userAgent);
        repository.save(log);
    }


    public void markClicked(Long logId) {
        repository.findById(logId).ifPresent(log -> {
            log.setClicked(true);
            repository.save(log);
        });
    }


    public List<TopQueryDto> getTopQueries(int limit) {
        return repository.findTopQueries(PageRequest.of(0, limit))
                .stream()
                .map(row -> new TopQueryDto((String)row[0], (Long)row[1]))
                .collect(Collectors.toList());
    }


    public List<ZeroResultQueryDto> getZeroResultQueries() {
        return repository.findZeroResultQueries()
                .stream()
                .map(row -> new ZeroResultQueryDto((String)row[0], (Long)row[1]))
                .collect(Collectors.toList());
    }
}
