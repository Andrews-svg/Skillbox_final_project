package com.example.searchengine.dto.adminLogs;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AdminAnalyticsDto {
    private List<TopQueryDto> topQueries;
    private List<ZeroResultQueryDto> zeroResultQueries;
    private int activeSessions;
    private Map<String, Object> systemHealth;
    private List<?> authLogs;
    private long totalVisits;
    private long totalUsers;

    private AdminAnalyticsDto(Builder builder) {
        this.topQueries = builder.topQueries;
        this.zeroResultQueries = builder.zeroResultQueries;
        this.activeSessions = builder.activeSessions;
        this.systemHealth = builder.systemHealth;
        this.authLogs = builder.authLogs;
        this.totalVisits = builder.totalVisits;
        this.totalUsers = builder.totalUsers;
    }

    public static Builder builder() {
        return new Builder();
    }


    public List<TopQueryDto> getTopQueries() { return topQueries; }
    public List<ZeroResultQueryDto> getZeroResultQueries() { return zeroResultQueries; }
    public int getActiveSessions() { return activeSessions; }
    public Map<String, Object> getSystemHealth() { return systemHealth; }
    public List<?> getAuthLogs() { return authLogs; }
    public long getTotalVisits() { return totalVisits; }
    public long getTotalUsers() { return totalUsers; }


    public void setTopQueries(List<TopQueryDto> topQueries) { this.topQueries = topQueries; }
    public void setZeroResultQueries(List<ZeroResultQueryDto> zeroResultQueries) {
        this.zeroResultQueries = zeroResultQueries;
    }
    public void setActiveSessions(int activeSessions) { this.activeSessions = activeSessions; }
    public void setSystemHealth(Map<String, Object> systemHealth) { this.systemHealth = systemHealth; }
    public void setAuthLogs(List<?> authLogs) { this.authLogs = authLogs; }
    public void setTotalVisits(long totalVisits) { this.totalVisits = totalVisits; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AdminAnalyticsDto that = (AdminAnalyticsDto) o;
        return activeSessions == that.activeSessions &&
                totalVisits == that.totalVisits &&
                totalUsers == that.totalUsers &&
                Objects.equals(topQueries, that.topQueries) &&
                Objects.equals(zeroResultQueries, that.zeroResultQueries) &&
                Objects.equals(systemHealth, that.systemHealth) &&
                Objects.equals(authLogs, that.authLogs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topQueries, zeroResultQueries,
                activeSessions, systemHealth, authLogs,
                totalVisits, totalUsers);
    }

    @Override
    public String toString() {
        return "AdminAnalyticsDto{" +
                "topQueries=" + topQueries +
                ", zeroResultQueries=" + zeroResultQueries +
                ", activeSessions=" + activeSessions +
                ", systemHealth=" + systemHealth +
                ", authLogs=" + authLogs +
                ", totalVisits=" + totalVisits +
                ", totalUsers=" + totalUsers +
                '}';
    }


    public static class Builder {
        private List<TopQueryDto> topQueries;
        private List<ZeroResultQueryDto> zeroResultQueries;
        private int activeSessions;
        private Map<String, Object> systemHealth;
        private List<?> authLogs;
        private long totalVisits;
        private long totalUsers;

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

        public Builder totalVisits(long totalVisits) {
            this.totalVisits = totalVisits;
            return this;
        }

        public Builder totalUsers(long totalUsers) {
            this.totalUsers = totalUsers;
            return this;
        }

        public AdminAnalyticsDto build() {
            return new AdminAnalyticsDto(this);
        }
    }
}