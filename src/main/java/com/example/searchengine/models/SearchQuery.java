package com.example.searchengine.models;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "search_queries", indexes = {
        @jakarta.persistence.Index(name = "idx_user_id", columnList = "user_id"),
        @jakarta.persistence.Index(name = "idx_queried_at", columnList = "queried_at"),
        @jakarta.persistence.Index(name = "idx_query", columnList = "query")
})
@EntityListeners(AuditingEntityListener.class)
public class SearchQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "query", nullable = false, length = 500)
    private String query;

    @Column(name = "site_filter")
    private String siteFilter;

    @Column(name = "result_count")
    private Integer resultCount;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @CreatedDate
    @Column(name = "queried_at", nullable = false, updatable = false)
    private Instant queriedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;


    public SearchQuery() {}

    public SearchQuery(AppUser user, String query) {
        this.user = user;
        this.query = query;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSiteFilter() {
        return siteFilter;
    }

    public void setSiteFilter(String siteFilter) {
        this.siteFilter = siteFilter;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Instant getQueriedAt() {
        return queriedAt;
    }

    public void setQueriedAt(Instant queriedAt) {
        this.queriedAt = queriedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    @Override
    public String toString() {
        return "SearchQuery{" +
                "id=" + id +
                ", user=" + (user != null ? user.getUsername() : "null") +
                ", query='" + query + '\'' +
                ", siteFilter='" + siteFilter + '\'' +
                ", resultCount=" + resultCount +
                ", executionTimeMs=" + executionTimeMs +
                ", queriedAt=" + queriedAt +
                ", ipAddress='" + ipAddress + '\'' +
                '}';
    }
}
