package com.example.searchengine.services;

import com.example.searchengine.models.ActivityActions;
import com.example.searchengine.models.statistics.UserActivityLog;
import com.example.searchengine.repositories.statistics.UserActivityRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserActivityService {

    private static final Logger log = LoggerFactory.getLogger(UserActivityService.class);

    private final UserActivityRepository activityRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public UserActivityService(UserActivityRepository activityRepository,
                               ObjectMapper objectMapper) {
        this.activityRepository = activityRepository;
        this.objectMapper = objectMapper;
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginSuccess(Long userId, String username, String ipAddress, String userAgent) {
        log.debug("Логирование успешного входа: {} с IP: {}", username, ipAddress);
        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.LOGIN_SUCCESS,
                ipAddress,
                userAgent,
                true
        );
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginFailed(Long userId, String username, String ipAddress,
                               String userAgent, String reason) {
        log.debug("Логирование неудачной попытки входа: {} с IP: {}, причина: {}",
                username, ipAddress, reason);

        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.LOGIN_FAILED,
                ipAddress,
                userAgent,
                false
        );
        logEntry.setActionDetail(reason);

        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRegistration(Long userId, String username, String ipAddress, String userAgent) {
        log.debug("Логирование регистрации: {} с IP: {}", username, ipAddress);
        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.REGISTRATION,
                ipAddress,
                userAgent,
                true
        );
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRegistrationFailed(String username, String ipAddress,
                                      String userAgent, String error) {
        log.debug("Логирование неудачной регистрации: {} с IP: {}, ошибка: {}",
                username, ipAddress, error);
        UserActivityLog logEntry = new UserActivityLog(
                null,
                username != null ? username : "anonymous",
                ActivityActions.REGISTRATION_FAILED,
                ipAddress,
                userAgent,
                false
        );
        logEntry.setActionDetail(error);
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivation(Long userId, String username, String ipAddress) {
        log.debug("Логирование активации аккаунта: {} с IP: {}", username, ipAddress);

        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.ACCOUNT_ACTIVATION,
                ipAddress,
                null,
                true
        );
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivationFailed(Long userId, String username, String ipAddress, String reason) {
        log.debug("Логирование неудачной активации: {} с IP: {}, причина: {}",
                username, ipAddress, reason);
        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.ACCOUNT_ACTIVATION_FAILED,
                ipAddress,
                null,
                false
        );
        logEntry.setActionDetail(reason);
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(Long userId, String username, String ipAddress, String userAgent) {
        log.debug("Логирование выхода из системы: {} с IP: {}", username, ipAddress);
        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.LOGOUT,
                ipAddress,
                userAgent,
                true
        );

        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTokenRefresh(Long userId, String username, String ipAddress) {
        log.debug("Логирование обновления токена: {} с IP: {}", username, ipAddress);
        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.TOKEN_REFRESH,
                ipAddress,
                null,
                true
        );
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logSearch(Long userId, String username, String ipAddress,
                          String query, Integer resultsCount, Long executionTimeMs,
                          Boolean success, String errorMessage) {
        log.debug("Логирование поискового запроса: '{}' от пользователя {}", query, username);

        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.SEARCH,
                ipAddress,
                null,
                success
        );
        logEntry.setExecutionTimeMs(executionTimeMs);
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("query", query);
        additionalInfo.put("resultsCount", resultsCount);
        if (errorMessage != null) {
            additionalInfo.put("error", errorMessage);
        }
        try {
            logEntry.setAdditionalInfo(objectMapper.writeValueAsString(additionalInfo));
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации дополнительной информации: {}", e.getMessage());
        }
        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIndexingStart(Long userId, String username, String ipAddress,
                                 String siteUrl, Boolean singleSite) {
        String action = singleSite ? ActivityActions.INDEXING_SINGLE_SITE
                : ActivityActions.INDEXING_START;

        log.debug("Логирование начала индексации: {} пользователем {}", siteUrl, username);

        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                action,
                ipAddress,
                null,
                true
        );

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("siteUrl", siteUrl);
        additionalInfo.put("singleSite", singleSite);

        try {
            logEntry.setAdditionalInfo(objectMapper.writeValueAsString(additionalInfo));
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации дополнительной информации: {}", e.getMessage());
        }

        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logIndexingComplete(Long userId, String username, String ipAddress,
                                    Integer sitesIndexed, Integer pagesIndexed,
                                    Long executionTimeMs) {
        log.debug("Логирование завершения индексации пользователем {}", username);

        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                ActivityActions.INDEXING_COMPLETE,
                ipAddress,
                null,
                true
        );

        logEntry.setExecutionTimeMs(executionTimeMs);

        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("sitesIndexed", sitesIndexed);
        additionalInfo.put("pagesIndexed", pagesIndexed);

        try {
            logEntry.setAdditionalInfo(objectMapper.writeValueAsString(additionalInfo));
        } catch (JsonProcessingException e) {
            log.error("Ошибка сериализации дополнительной информации: {}", e.getMessage());
        }

        activityRepository.save(logEntry);
    }


    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(Long userId, String username, String action,
                          String ipAddress, String userAgent, Boolean success,
                          Map<String, Object> additionalInfo) {
        log.debug("Логирование действия: {} от пользователя {}", action, username);

        UserActivityLog logEntry = new UserActivityLog(
                userId,
                username,
                action,
                ipAddress,
                userAgent,
                success
        );

        if (additionalInfo != null && !additionalInfo.isEmpty()) {
            try {
                logEntry.setAdditionalInfo(objectMapper.writeValueAsString(additionalInfo));
            } catch (JsonProcessingException e) {
                log.error("Ошибка сериализации дополнительной информации: {}", e.getMessage());
            }
        }

        activityRepository.save(logEntry);
    }


    @Transactional
    public int cleanOldLogs(Instant olderThan) {
        log.info("Очистка логов старше: {}", olderThan);

        List<UserActivityLog> oldLogs = activityRepository
                .findByTimestampBetweenOrderByTimestampDesc(olderThan, Instant.now());

        activityRepository.deleteAll(oldLogs);

        log.info("Удалено {} старых записей", oldLogs.size());
        return oldLogs.size();
    }
}