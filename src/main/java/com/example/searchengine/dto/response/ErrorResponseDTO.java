package com.example.searchengine.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErrorResponseDTO {
    private LocalDateTime timestamp;
    private int code;
    private String message;

    public ErrorResponseDTO(LocalDateTime timestamp, int code, String message) {
        this.timestamp = timestamp;
        this.code = code;
        this.message = message;
    }
}