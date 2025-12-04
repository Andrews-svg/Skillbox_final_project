package com.example.searchengine.dto.statistics.response;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

public class ErrorResponseDTO {


    private LocalDateTime timestamp;

    private long code;

    private String message;

    private Map<String, Object> details;


    public ErrorResponseDTO() {
        this.timestamp = LocalDateTime.now();
    }


    public ErrorResponseDTO(long code, String message) {
        this();
        this.code = code;
        this.message = message;
    }


    public ErrorResponseDTO(LocalDateTime timestamp,
                            long code, String message) {
        this(timestamp, code, message, null);
    }


    public ErrorResponseDTO(LocalDateTime timestamp, long code,
                            String message, Map<String, Object> details) {
        this.timestamp = timestamp;
        this.code = code;
        this.message = message;
        this.details = details;
    }


    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public long getCode() {
        return code;
    }

    public void setCode(long code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public void setDetails(Map<String, Object> details) {
        this.details = details;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ErrorResponseDTO that)) return false;
        return code == that.code &&
                Objects.equals(timestamp, that.timestamp) &&
                Objects.equals(message, that.message) &&
                Objects.equals(details, that.details);
    }


    @Override
    public int hashCode() {
        return Objects.hash(timestamp, code, message, details);
    }


    @Override
    public String toString() {
        return "ErrorResponseDTO{" +
                "timestamp=" + timestamp +
                ", code=" + code +
                ", message='" + message + '\'' +
                ", details=" + details +
                '}';
    }
}