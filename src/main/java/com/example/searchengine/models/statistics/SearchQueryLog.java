package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import java.time.LocalDateTime;

@Entity
@Table(name = "search_query_logs", indexes = {
        @Index(name = "idx_search_user_id", columnList = "user_id"),
        @Index(name = "idx_search_query", columnList = "query"),
        @Index(name = "idx_search_timestamp", columnList = "search_time"),
        @Index(name = "idx_search_site", columnList = "site_url")
})
public class SearchQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 100)
    private String username;

    @Column(nullable = false, length = 500)
    private String query;

    @Column(name = "site_url", length = 255)
    private String siteUrl;

    @Column(name = "search_time", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "results_count")
    private Integer resultsCount;

    @Column(name = "clicked")
    private Boolean clicked;

    @Column(name = "response_time_ms")
    private Long responseTimeMs;

    @Column(name = "success")
    private Boolean success;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "user_ip", length = 45)
    private String userIp;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "advanced_search")
    private Boolean advancedSearch;

    @Column(name = "filters", length = 1000)
    private String filters;


    public SearchQueryLog() {}

    public SearchQueryLog(String query, LocalDateTime timestamp) {
        this.query = query;
        this.timestamp = timestamp;
    }


    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getSiteUrl() { return siteUrl; }
    public void setSiteUrl(String siteUrl) { this.siteUrl = siteUrl; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getResultsCount() { return resultsCount; }
    public void setResultsCount(Integer resultsCount) { this.resultsCount = resultsCount; }

    public Boolean getClicked() { return clicked; }
    public void setClicked(Boolean clicked) { this.clicked = clicked; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getUserIp() { return userIp; }
    public void setUserIp(String userIp) { this.userIp = userIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Boolean getAdvancedSearch() { return advancedSearch; }
    public void setAdvancedSearch(Boolean advancedSearch) { this.advancedSearch = advancedSearch; }

    public String getFilters() { return filters; }
    public void setFilters(String filters) { this.filters = filters; }
}