package com.example.searchengine.services;

import com.example.searchengine.dto.AuthLogEntry;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private static final int MAX_LOG_ENTRIES = 100;

    private final Queue<AuthLogEntry> authLogs = new ConcurrentLinkedQueue<>();
    private final SessionRegistry sessionRegistry;

    public AuthService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }


    public void logLogin(HttpServletRequest request, String username) {
        AuthLogEntry entry = new AuthLogEntry(
                "LOGIN",
                username,
                getClientIp(request),
                request.getHeader("User-Agent"),
                LocalDateTime.now(),
                true,
                null
        );

        addToLog(entry);
        logger.info("✅ Успешный вход: {} с IP {}", username, entry.getIp());
    }


    public void logLoginFailed(HttpServletRequest request, String username, String reason) {
        AuthLogEntry entry = new AuthLogEntry(
                "LOGIN_FAILED",
                username != null ? username : "unknown",
                getClientIp(request),
                request.getHeader("User-Agent"),
                LocalDateTime.now(),
                false,
                reason
        );
        addToLog(entry);
        logger.warn("⚠️ Неудачная попытка входа: {} с IP {}, причина: {}",
                username != null ? username : "unknown", entry.getIp(), reason);
    }


    public void logLogout(HttpServletRequest request, String username) {
        AuthLogEntry entry = new AuthLogEntry(
                "LOGOUT",
                username,
                getClientIp(request),
                request.getHeader("User-Agent"),
                LocalDateTime.now(),
                true,
                null
        );
        addToLog(entry);
        logger.info("✅ Выход: {} с IP {}", username, entry.getIp());
    }


    public void logAction(String username, String action, String details) {
        AuthLogEntry entry = new AuthLogEntry(
                "ACTION",
                username,
                "system",
                "system",
                LocalDateTime.now(),
                true,
                action + ": " + details
        );
        addToLog(entry);
        logger.debug("📝 Действие {} от пользователя {}", action, username);
    }


    public List<AuthLogEntry> getRecentLogs(int limit) {
        List<AuthLogEntry> result = new ArrayList<>(authLogs);
        result.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
        return result.subList(0, Math.min(limit, result.size()));
    }


    public String getCurrentUsername() {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.
                        getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) auth.getPrincipal()).getUsername();
        }
        return auth != null ? auth.getName() : "anonymous";
    }




    public int getActiveSessionCount() {
        return sessionRegistry.getAllPrincipals().stream()
                .mapToInt(principal ->
                        sessionRegistry.getAllSessions(principal,
                                false).size())
                .sum();
    }


    public List<Map<String, Object>> getActiveUsers() {
        List<Map<String, Object>> activeUsers = new ArrayList<>();
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String username = principal instanceof UserDetails ?
                    ((UserDetails) principal).getUsername() : principal.toString();
            int sessionCount = sessionRegistry.getAllSessions(principal, false).size();
            if (sessionCount > 0) {
                Map<String, Object> userInfo = new HashMap<>();
                userInfo.put("username", username);
                userInfo.put("sessionCount", sessionCount);
                activeUsers.add(userInfo);
            }
        }
        return activeUsers;
    }


    public int expireUserSessions(String username) {
        int expiredCount = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String principalName = principal instanceof UserDetails ?
                    ((UserDetails) principal).getUsername() : principal.toString();
            if (principalName.equals(username)) {
                for (var sessionInfo : sessionRegistry.getAllSessions(principal, false)) {
                    sessionInfo.expireNow();
                    expiredCount++;
                }
            }
        }
        if (expiredCount > 0) {
            logger.info("✅ Завершено {} сессий пользователя {}", expiredCount, username);
        }
        return expiredCount;
    }



    private void addToLog(AuthLogEntry entry) {
        authLogs.offer(entry);
        while (authLogs.size() > MAX_LOG_ENTRIES) {
            authLogs.poll();
        }
    }


    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
