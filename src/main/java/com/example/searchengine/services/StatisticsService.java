package com.example.searchengine.services;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.dto.statistics.responses.DetailedStatisticsItem;
import com.example.searchengine.dto.statistics.responses.StatisticsData;
import com.example.searchengine.dto.statistics.responses.TotalStatistics;
import com.example.searchengine.services.indexing.IndexingService;
import com.example.searchengine.services.indexing.IndexingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexingState indexingState;

    public StatisticsService(SiteService siteService,
                             PageService pageService,
                             LemmaService lemmaService,
                             IndexingService indexingService, IndexingState indexingState) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexingState = indexingState;
        logger.info("StatisticsService initialized");
    }


    public StatisticsData getStatistics() {
        logger.debug("Начало сбора статистики");
        TotalStatistics total = getTotalStatistics();
        List<DetailedStatisticsItem> detailed = getDetailedStatistics();
        StatisticsData result = new StatisticsData(total, detailed);
        logger.info("Статистика собрана: сайтов={}, страниц={}, лемм={}, индексация={}",
                total.getSites(), total.getPages(), total.getLemmas(), total.isIndexing());
        return result;
    }


    private TotalStatistics getTotalStatistics() {
        long sitesCount = siteService.getTotalSites();
        long pagesCount = pageService.getTotalPages();
        long lemmasCount = lemmaService.getTotalLemmas();
        boolean isIndexing = indexingState.isActive();
        return new TotalStatistics(sitesCount, pagesCount, lemmasCount, isIndexing);
    }


    private List<DetailedStatisticsItem> getDetailedStatistics() {
        List<Site> allSites = siteService.findAll();
        List<DetailedStatisticsItem> detailedItems = new ArrayList<>();
        if (allSites.isEmpty()) {
            logger.debug("Нет сайтов для детальной статистики");
            return detailedItems;
        }
        for (Site site : allSites) {
            try {
                DetailedStatisticsItem item = createDetailedItem(site);
                detailedItems.add(item);
                logger.trace("Добавлена статистика для сайта: {}", site.getUrl());
            } catch (Exception e) {
                logger.error("Ошибка при сборе статистики для сайта {}: {}",
                        site.getUrl(), e.getMessage(), e);
            }
        }
        logger.debug("Собрана статистика для {} сайтов", detailedItems.size());
        return detailedItems;
    }


    private DetailedStatisticsItem createDetailedItem(Site site) {
        long pages = pageService.countBySite(site);
        long lemmas = lemmaService.countBySite(site);
        String status = site.getStatus() != null ? site.getStatus().name() : Status.FAILED.name();
        Long statusTime = site.getStatusTime() != null
                ? Timestamp.valueOf(site.getStatusTime()).getTime()
                : null;
        String error = site.getLastError();
        if (error != null && error.trim().isEmpty()) {
            error = null;
        }
        return new DetailedStatisticsItem(
                site.getUrl(),
                site.getName(),
                status,
                statusTime,
                error,
                pages,
                lemmas
        );
    }
}
