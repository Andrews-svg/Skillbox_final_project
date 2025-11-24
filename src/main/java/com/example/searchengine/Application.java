package com.example.searchengine;

import com.example.searchengine.config.SitesList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@EnableConfigurationProperties(SitesList.class)
@EnableJpaRepositories(basePackages = "com.example.searchengine.repository")
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        logger.info("Starting application...");
        try {
            SpringApplication.run(Application.class, args);
            logger.info("Application started successfully.");
        } catch (Exception e) {
            logger.error("Application failed to start: ", e);
        }
    }
}
