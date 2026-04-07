package com.example.searchengine.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AuthLogEntry {
    private final String eventType;
    private final String username;
    private final String ip;
    private final String userAgent;
    private final LocalDateTime timestamp;
    private final boolean success;
    private final String details;

    public AuthLogEntry(String eventType, String username, String ip,
                        String userAgent, LocalDateTime timestamp,
                        boolean success, String details) {
        this.eventType = eventType;
        this.username = username;
        this.ip = ip;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
        this.success = success;
        this.details = details;
    }


    public String getEventType() { return eventType; }
    public String getUsername() { return username; }
    public String getIp() { return ip; }
    public String getUserAgent() { return userAgent; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public boolean isSuccess() { return success; }
    public String getDetails() { return details; }

    public String getFormattedTimestamp() {
        return timestamp.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"));
    }

    public String getEventTypeIcon() {
        return switch (eventType) {
            case "LOGIN" -> "✅";
            case "LOGIN_FAILED" -> "❌";
            case "LOGOUT" -> "👋";
            case "ACTION" -> "📝";
            default -> "•";
        };
    }

    public String getEventTypeRu() {
        return switch (eventType) {
            case "LOGIN" -> "Вход";
            case "LOGIN_FAILED" -> "Ошибка входа";
            case "LOGOUT" -> "Выход";
            case "ACTION" -> "Действие";
            default -> eventType;
        };
    }
}
