package com.example.searchengine.statisticsResponse;

import com.example.searchengine.models.Site;
import com.example.searchengine.dto.statistics.StatisticsData;

import java.util.List;

public class DashboardResponse {
    private List<Site> sites;
    private StatisticsData statistics;


    public List<Site> getSites() {
        return sites;
    }

    public void setSites(List<Site> sites) {
        this.sites = sites;
    }

    public StatisticsData getStatistics() {
        return statistics;
    }

    public void setStatistics(StatisticsData statistics) {
        this.statistics = statistics;
    }
}
