package com.example.searchengine.services;

import com.example.searchengine.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.Site;
import com.example.searchengine.dto.statistics.DetailedStatisticsItem;
import com.example.searchengine.dto.statistics.StatisticsData;
import com.example.searchengine.dto.statistics.StatisticsReport;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger logger =
            LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final SiteRepository siteRepository;


    @Autowired
    public StatisticsServiceImpl(SiteService siteService,
                                 PageService pageService,
                                 LemmaService lemmaService, SiteRepository siteRepository) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.siteRepository = siteRepository;

        logger.info("StatisticsServiceImpl initialized.");
    }

    @Override
    public StatisticsData getStatistics() {
        logger.info("Начало сбора статистики");
        StatisticsReport total = getFullStats();
        logger.debug("Общая статистика собрана: {}", total);
        List<Site> allSites = siteRepository.findAll();
        logger.debug("Всего найдено {} сайтов.", allSites.size());
        List<DetailedStatisticsItem> detailedItems = new ArrayList<>();
        int batchSize = 3;
        for (int i = 0; i < allSites.size(); i += batchSize) {
            List<Site> currentBatch = allSites.subList(i, Math.min(allSites.size(), i + batchSize));
            Map<Long, Long> pagesCountMap = pageService.countPagesGroupedBySite(currentBatch);
            Map<Long, Long> lemmasCountMap = lemmaService.countLemmasGroupedBySite(currentBatch);
            for (Site site : currentBatch) {
                try {
                    DetailedStatisticsItem item = getDetailed(site, pagesCountMap, lemmasCountMap);
                    detailedItems.add(item);
                } catch (Exception e) {
                    logger.error("Ошибка при обработке сайта '{}': {}", site.getName(), e.getMessage());
                }
            }
        }
        StatisticsData result = new StatisticsData(total, detailedItems);
        logger.info("Сбор статистики завершён успешно.");
        return result;
    }


    public DetailedStatisticsItem getDetailed(Site site, Map<Long, Long> pagesCountMap,
                                              Map<Long, Long> lemmasCountMap) {
        logger.debug("Формирование детализированной статистики для сайта: {}", site.getUrl());
        Long pages = pagesCountMap.getOrDefault(site.getId(), 0L);
        Long lemmas = lemmasCountMap.getOrDefault(site.getId(), 0L);
        DetailedStatisticsItem detailedItem = new DetailedStatisticsItem(
                site.getUrl(), site.getName(), site.getStatus(),
                getStatusTime(site), site.getLastError(), pages.intValue(), lemmas.intValue());
        logger.debug("Детализированная статистика сформирована: {}", detailedItem);
        return detailedItem;
    }


    private long getStatusTime(Site site) {
        if (site.getStatusTime() != null) {
            ZonedDateTime zdt = site.getStatusTime()
                    .atZone(ZoneId.systemDefault());
            return zdt.toInstant().toEpochMilli();
        }
        return 0L;
    }


    public StatisticsReport getFullStats() {
        long sitesCount = siteService.getTotalSites();
        long pagesCount = pageService.getTotalPages();
        long lemmasCount = lemmaService.getTotalLemmas();

        boolean isIndexing = siteService.isAnySiteIndexing();
        StatisticsReport report = new StatisticsReport();
        report.setSites(sitesCount);
        report.setPages(pagesCount);
        report.setLemmas(lemmasCount);
        report.setIndexing(isIndexing);
        logger.debug("Общая статистика: " +
                        "sitesCount={}, pagesCount={}, " +
                        "lemmasCount={}, isIndexing={}",
                sitesCount, pagesCount, lemmasCount, isIndexing);
        return report;
    }
}
