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
    public long siteNumber() {
        return 5;
    }

    @Bean
    public long pageNumber() {
        return 100;
    }

    @Bean
    public long lemmaNumber() {
        return 50;
    }

    @Bean
    public Boolean isIndexing() {
        return true;
    }


    @Bean
    public StatisticsReport total(Long siteNumber, Long pageNumber,
                                  Long lemmaNumber, Boolean isIndexing) {
        StatisticsReport report = new StatisticsReport();
        report.setSites(siteNumber);
        report.setPages(pageNumber);
        report.setLemmas(lemmaNumber);
        report.setIndexing(isIndexing);
        return report;}

    @Bean
    public String siteUrlRegex() {

        return "https://www\\.(lenta\\.ru|skillbox\\.ru|playback\\.ru)";
    }

    @Bean
    public String exampleDomainRegex() {

        return "https://www\\.(example\\.com|otherdomain\\.com)";
    }
}
