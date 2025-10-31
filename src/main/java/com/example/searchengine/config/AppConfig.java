package com.example.searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import com.example.searchengine.dto.statistics.StatisticsReport;

@Configuration
public class AppConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public Integer siteNumber() {
        return 5;
    }

    @Bean
    public Integer pageNumber() {
        return 100;
    }

    @Bean
    public Integer lemmaNumber() {
        return 50;
    }

    @Bean
    public Boolean isIndexing() {
        return true;
    }

    @Bean
    public StatisticsReport total(Integer siteNumber,
                                  Integer pageNumber, Integer lemmaNumber,
                                  Boolean isIndexing) {
        return new StatisticsReport(siteNumber, pageNumber,
                lemmaNumber, isIndexing);
    }

    @Bean
    public String siteUrlRegex() {

        return "https://www\\.(lenta\\.ru|skillbox\\.ru|playback\\.ru)";
    }

    @Bean
    public String exampleDomainRegex() {

        return "https://www\\.(example\\.com|otherdomain\\.com)";
    }
}
