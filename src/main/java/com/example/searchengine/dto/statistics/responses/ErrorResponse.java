package com.example.searchengine.dto.statisticsResponse;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Component
public class ErrorResponse {
    private boolean success;
    private Integer httpStatusCode;
    private String error;
    private String message;
    private String details;
    private String path;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalInfo;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.additionalInfo = new HashMap<>();
    }

    public ErrorResponse(String message, String details) {
        this();
        this.success = false;
        this.message = message;
        this.details = details;
    }

    public ErrorResponse(String error) {
        this();
        this.success = false;
        this.error = error;
    }

    public ErrorResponse(boolean success, String message) {
        this();
        this.success = success;
        this.message = message;
    }

    public ErrorResponse(Integer httpStatusCode, String message) {
        this();
        this.success = false;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }

    public ErrorResponse(Integer httpStatusCode, String error, String message) {
        this();
        this.success = false;
        this.httpStatusCode = httpStatusCode;
        this.error = error;
        this.message = message;
    }


    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    public void setHttpStatusCode(Integer httpStatusCode) {
        this.httpStatusCode = httpStatusCode;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getAdditionalInfo() {
        return additionalInfo;
    }

    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }

    public void addAdditionalInfo(String key, Object value) {
        this.additionalInfo.put(key, value);
    }

    @Override
    public String toString() {
        return "ErrorResponse{" +
                "success=" + success +
                ", httpStatusCode=" + httpStatusCode +
                ", error='" + error + '\'' +
                ", message='" + message + '\'' +
                ", details='" + details + '\'' +
                ", path='" + path + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}