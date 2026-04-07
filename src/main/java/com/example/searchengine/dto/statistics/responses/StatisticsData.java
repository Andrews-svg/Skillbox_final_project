package com.example.searchengine.dto.statistics.responses;

import java.util.List;

public class StatisticsData {

    private final TotalStatistics total;
    private final List<DetailedStatisticsItem> detailed;


    public StatisticsData(TotalStatistics total,
                          List<DetailedStatisticsItem> detailed) {
        this.total = total;
        this.detailed = detailed;
    }


    public TotalStatistics getTotal() {
        return total;
    }

    public List<DetailedStatisticsItem> getDetailed() {
        return detailed;
    }


    public static class TotalStatistics {
        private final long sites;


        public TotalStatistics(long sites) {
            this.sites = sites;
        }


        public long getSites() {
            return sites;
        }
    }
}