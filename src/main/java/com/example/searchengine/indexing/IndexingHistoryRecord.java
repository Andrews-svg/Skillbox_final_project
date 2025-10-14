package com.example.searchengine.indexing;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.example.searchengine.models.Status;

@Entity
@Table(name = "indexing_history")
public class IndexingHistoryRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String url;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public IndexingHistoryRecord(String url, LocalDateTime timestamp, Status status) {
        this.url = url;
        this.timestamp = timestamp;
        this.status = status;
    }

    public IndexingHistoryRecord() {}

    public Integer getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Status getStatus() {
        return status;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndexingHistoryRecord)) return false;
        IndexingHistoryRecord that = (IndexingHistoryRecord) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "IndexingHistoryRecord{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", timestamp=" + timestamp +
                ", status=" + status +
                '}';
    }
}
