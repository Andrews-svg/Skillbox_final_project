package com.example.searchengine.dto.statistics;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
public class StatisticsData {

    private StatisticsReport total;
    private List<DetailedStatisticsItem> detailed;

    @Autowired
    public StatisticsData(StatisticsReport total, List<DetailedStatisticsItem> detailed) {
        this.total = total;
        this.detailed = detailed;
    }

    public List<DetailedStatisticsItem> getDetailed() {
        return this.detailed;
    }

    public StatisticsReport getTotal() {
        return this.total;
    }

    public void setTotal(StatisticsReport total) {
        this.total = total;
    }

    public void setDetailed(List<DetailedStatisticsItem> detailed) {
        this.detailed = detailed;
    }
  }
