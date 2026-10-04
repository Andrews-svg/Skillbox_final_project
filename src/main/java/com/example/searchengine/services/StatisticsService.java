package com.example.searchengine.services;

import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.dto.statistics.responses.DetailedStatisticsItem;
import com.example.searchengine.dto.statistics.responses.StatisticsData;
import com.example.searchengine.dto.statistics.responses.TotalStatistics;
import com.example.searchengine.services.indexing.IndexingState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class StatisticsService {

    private static final Logger logger = LoggerFactory.getLogger(StatisticsService.class);

    private final SiteService siteService;
    private final IndexingState indexingState;

    public StatisticsService(SiteService siteService,
                            IndexingState indexingState) {
        this.siteService = siteService;
        this.indexingState = indexingState;
        logger.info("StatisticsService initialized");
    }


    public StatisticsData getStatistics() {
        logger.debug("Начало сбора статистики");
        TotalStatistics total = getTotalStatistics();
        List<DetailedStatisticsItem> detailed = getDetailedStatistics();
        StatisticsData result = new StatisticsData(total, detailed);
        logger.info("Статистика собрана (live): сайтов={}, страниц={}, лемм={}, индексация={}",
                total.getSites(), total.getPages(), total.getLemmas(), total.isIndexing());
        return result;
    }


    private TotalStatistics getTotalStatistics() {
        long sitesCount = indexingState.getSessionSiteIds().size();
        long pagesCount = indexingState.getTotalPages();
        long lemmasCount = indexingState.getTotalLemmas();
        boolean isIndexing = indexingState.isActive();
        return new TotalStatistics(sitesCount, pagesCount, lemmasCount, isIndexing);
    }


    private List<DetailedStatisticsItem> getDetailedStatistics() {
        Set<Long> sessionSiteIds = indexingState.getSessionSiteIds();
        List<DetailedStatisticsItem> detailedItems = new ArrayList<>();

        if (sessionSiteIds.isEmpty()) {
            logger.debug("Live-сессия пуста — нет сайтов для детальной статистики");
            return detailedItems;
        }

        for (Long siteId : sessionSiteIds) {
            try {
                Optional<Site> siteOpt = siteService.findById(siteId);
                if (siteOpt.isEmpty()) {
                    logger.warn("Сайт с id={} не найден в БД, пропускаем", siteId);
                    continue;
                }
                DetailedStatisticsItem item = createDetailedItem(siteOpt.get());
                detailedItems.add(item);
                logger.trace("Добавлена live-статистика для сайта: {}", siteOpt.get().getUrl());
            } catch (Exception e) {
                logger.error("Ошибка при сборе статистики для сайта id={}: {}",
                        siteId, e.getMessage(), e);
            }
        }
        logger.debug("Собрана live-статистика для {} сайтов", detailedItems.size());
        return detailedItems;
    }


    private DetailedStatisticsItem createDetailedItem(Site site) {
        long pages = indexingState.getSessionPages(site.getId());
        long lemmas = indexingState.getSessionLemmas(site.getId());

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