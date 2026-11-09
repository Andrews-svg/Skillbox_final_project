package com.example.searchengine.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class DbCredentialsValidator {

    private static final Logger logger = LoggerFactory.getLogger(DbCredentialsValidator.class);
    private static final String PROD_PROFILE = "prod";
    private static final String FORBIDDEN_USER = "root";

    private final Environment environment;
    private final String dbUsername;

    public DbCredentialsValidator(
            Environment environment,
            @Value("${spring.datasource.username:}") String dbUsername) {
        this.environment = environment;
        this.dbUsername = dbUsername;
    }

    @PostConstruct
    public void validate() {
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains(PROD_PROFILE);
        if (!isProd) {
            logger.debug("DB credentials check skipped (profile is not prod)");
            return;
        }
        if (dbUsername == null || dbUsername.isBlank()) {
            throw new IllegalStateException(
                    "DB_USER is not set in prod profile. Set DB_USER to a dedicated application user.");
        }
        if (FORBIDDEN_USER.equalsIgnoreCase(dbUsername.trim())) {
            throw new IllegalStateException(
                    "DB_USER=root is forbidden in prod profile. Set DB_USER to a dedicated application user.");
        }
        logger.debug("DB credentials check passed (prod, user={})", dbUsername);
    }
}