package com.example.searchengine.dto;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminAnalyticsDto {
    private List<TopQueryDto> topQueries;
    private List<ZeroResultQueryDto> zeroResultQueries;
    private int activeSessions;
    private Map<String, Object> systemHealth;
    private List<?> authLogs;

    private AdminAnalyticsDto(Builder builder) {
        this.topQueries = builder.topQueries;
        this.zeroResultQueries = builder.zeroResultQueries;
        this.activeSessions = builder.activeSessions;
        this.systemHealth = builder.systemHealth;
        this.authLogs = builder.authLogs;
    }

    public static Builder builder() {
        return new Builder();
    }


    public List<TopQueryDto> getTopQueries() { return topQueries; }
    public List<ZeroResultQueryDto> getZeroResultQueries() { return zeroResultQueries; }
    public int getActiveSessions() { return activeSessions; }
    public Map<String, Object> getSystemHealth() { return systemHealth; }
    public List<?> getAuthLogs() { return authLogs; }


    public void setTopQueries(List<TopQueryDto> topQueries) { this.topQueries = topQueries; }
    public void setZeroResultQueries(List<ZeroResultQueryDto> zeroResultQueries) {
        this.zeroResultQueries = zeroResultQueries;
    }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
    public void setSystemHealth(Map<String, Object> systemHealth) { this.systemHealth = systemHealth; }
    public void setAuthLogs(List<?> authLogs) { this.authLogs = authLogs; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminAnalyticsDto that = (AdminAnalyticsDto) o;
        return activeSessions == that.activeSessions &&
                Objects.equals(topQueries, that.topQueries) &&
                Objects.equals(zeroResultQueries, that.zeroResultQueries) &&
                Objects.equals(systemHealth, that.systemHealth) &&
                Objects.equals(authLogs, that.authLogs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topQueries, zeroResultQueries,
                activeSessions, systemHealth, authLogs);
    }

    @Override
    public String toString() {
        return "AdminAnalyticsDto{" +
                "topQueries=" + topQueries +
                ", zeroResultQueries=" + zeroResultQueries +
                ", activeSessions=" + activeSessions +
                ", systemHealth=" + systemHealth +
                ", authLogs=" + authLogs +
                '}';
    }

    // Вложенный класс Builder
    public static class Builder {
        private List<TopQueryDto> topQueries;
        private List<ZeroResultQueryDto> zeroResultQueries;
        private int activeSessions;
        private Map<String, Object> systemHealth;
        private List<?> authLogs;

        public Builder topQueries(List<TopQueryDto> topQueries) {
            this.topQueries = topQueries;
            return this;
        }

        public Builder zeroResultQueries(List<ZeroResultQueryDto> zeroResultQueries) {
            this.zeroResultQueries = zeroResultQueries;
            return this;
        }

        public Builder activeSessions(int activeSessions) {
            this.activeSessions = activeSessions;
            return this;
        }

        public Builder systemHealth(Map<String, Object> systemHealth) {
            this.systemHealth = systemHealth;
            return this;
        }

        public Builder authLogs(List<?> authLogs) {
            this.authLogs = authLogs;
            return this;
        }

        public AdminAnalyticsDto build() {
            return new AdminAnalyticsDto(this);
        }
    }
}