package com.example.searchengine.dto.statistics.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;


@Component
public class ErrorResponse {

    @JsonProperty("result")
    private boolean result = false;

    @JsonProperty("error")
    private String error;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("message")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String message;

    @JsonProperty("details")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String details;

    @JsonProperty("path")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String path;

    @JsonProperty("developerMessage")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String developerMessage;

    @JsonProperty("additionalInfo")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> additionalInfo;


    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
        this.additionalInfo = new HashMap<>();
    }


    public ErrorResponse(LocalDateTime timestamp, Integer code, String error) {
        this();
        this.timestamp = timestamp;
        this.code = code;
        this.error = error;
    }


    public ErrorResponse(Integer code, String error) {
        this(LocalDateTime.now(), code, error);
    }


    public ErrorResponse(String error) {
        this(LocalDateTime.now(), 400, error);
    }


    public static ErrorResponse httpError(int httpStatusCode, String errorMessage) {
        return new ErrorResponse(LocalDateTime.now(), httpStatusCode, errorMessage);
    }

    public static ErrorResponse validationError(String errorMessage) {
        return new ErrorResponse(LocalDateTime.now(), 400, errorMessage);
    }

    public static ErrorResponse serverError(String errorMessage) {
        return new ErrorResponse(LocalDateTime.now(), 500, errorMessage);
    }


    public static ErrorResponse withDeveloperMessage(int code, String message,
                                                     String developerMessage) {
        ErrorResponse response = new ErrorResponse(code, message);
        response.setDeveloperMessage(developerMessage);
        return response;
    }


    public boolean isResult() { return result; }
    public void setResult(boolean result) { this.result = result; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }


    public String getDeveloperMessage() {
        return developerMessage;
    }


    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }


    public Map<String, Object> getAdditionalInfo() { return additionalInfo; }
    public void setAdditionalInfo(Map<String, Object> additionalInfo) {
        this.additionalInfo = additionalInfo;
    }


    public void addAdditionalInfo(String key, Object value) {
        if (this.additionalInfo == null) {
            this.additionalInfo = new HashMap<>();
        }
        this.additionalInfo.put(key, value);
    }


    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("result", this.result);
        map.put("error", this.error);
        map.put("timestamp", this.timestamp);
        map.put("code", this.code);
        if (this.message != null) map.put("message", this.message);
        if (this.details != null) map.put("details", this.details);
        if (this.path != null) map.put("path", this.path);
        if (this.developerMessage != null) map.put("developerMessage", this.developerMessage);
        if (this.additionalInfo != null && !this.additionalInfo.isEmpty()) {
            map.putAll(this.additionalInfo);
        }
        return map;
    }


    @Override
    public String toString() {
        return "ErrorResponse{" +
                "result=" + result +
                ", error='" + error + '\'' +
                ", timestamp=" + timestamp +
                ", code=" + code +
                (message != null ? ", message='" + message + '\'' : "") +
                (details != null ? ", details='" + details + '\'' : "") +
                (developerMessage != null ? ", developerMessage='" + developerMessage + '\'' : "") +
                '}';
    }
}