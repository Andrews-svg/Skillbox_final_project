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
    public Long siteNumber() {
        return 5L;
    }

    @Bean
    public Long pageNumber() {
        return 100L;
    }

    @Bean
    public Long lemmaNumber() {
        return 50L;
    }

    @Bean
    public Boolean isIndexing() {
        return true;
    }

    @Bean
    public StatisticsReport total(Long siteNumber,
                                  Long pageNumber, Long lemmaNumber,
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
