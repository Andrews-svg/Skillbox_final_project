package com.example.searchengine.statisticsResponse;

public class SuccessResponse {
    private final boolean result;
    private final String message;


    public SuccessResponse() {
        this.result = true;
        this.message = "Успешно выполнено";
    }


    public SuccessResponse(String message) {
        this.result = true;
        this.message = message;
    }


    public SuccessResponse(boolean result, String message) {
        this.result = result;
        this.message = message;
    }


    public boolean isResult() {
        return result;
    }


    public String getMessage() {
        return message;
    }
}