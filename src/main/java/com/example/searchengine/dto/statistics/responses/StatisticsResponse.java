package com.example.searchengine.dto.statisticsResponse;

import com.example.searchengine.dto.statistics.responses.StatisticsData;


public class StatisticsResponse {
    boolean result;
    StatisticsData statistics;

    public StatisticsResponse(boolean result, StatisticsData statistics) {
        this.result = result;
        this.statistics = statistics;
    }

    public StatisticsResponse() {
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public StatisticsData getStatistics() {
        return statistics;
    }

    public void setStatistics(StatisticsData statistics) {
        this.statistics = statistics;
    }
}
