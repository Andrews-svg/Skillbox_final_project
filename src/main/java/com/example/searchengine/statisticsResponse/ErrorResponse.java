package com.example.searchengine.statisticsResponse;


import org.springframework.stereotype.Component;

@Component
public class ErrorResponse {
    private boolean success;
    private Integer httpStatusCode;
    private String error;
    private String message;
    private String details;


    public ErrorResponse() {}

    public ErrorResponse(String message, String details) {
        this.success = false;
        this.message = message;
        this.details = details;
    }

    public ErrorResponse(String error) {
        this.success = false;
        this.error = error;
    }


    public ErrorResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }


    public ErrorResponse(Integer httpStatusCode, String message) {
        this.success = false;
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }

    // Стандартные геттеры и сеттеры
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
}
