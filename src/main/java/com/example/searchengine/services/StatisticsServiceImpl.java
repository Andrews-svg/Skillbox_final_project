package com.example.searchengine.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.dao.LemmaDao;
import com.example.searchengine.dao.PageDao;
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
    private final SitesList sites;
    private final LemmaDao lemmaDao;
    private final PageDao pageDao;

    @Autowired
    public StatisticsServiceImpl(SiteService siteService,
                                 PageService pageService,
                                 LemmaDao lemmaDao,
                                 LemmaService lemmaService,
                                 SitesList sites, PageDao pageDao) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.lemmaDao = lemmaDao;
        this.sites = sites;
        this.pageDao = pageDao;
        logger.info("StatisticsServiceImpl initialized.");
    }

    @Override
    public StatisticsData getStatistics() {
        logger.info("Начало сбора статистики");

        StatisticsReport total = getTotal();
        logger.debug("Общая статистика собрана: {}", total);

        List<Site> allSites = siteService.findAllSites();
        logger.debug("Всего найдено {} сайтов.", allSites.size());

        List<DetailedStatisticsItem> detailedItems = new ArrayList<>();

        int batchSize = 3;

        for (int i = 0; i < allSites.size(); i += batchSize) {
            List<Site> currentBatch = allSites.subList(i, Math.min(allSites.size(), i + batchSize));

            Map<Integer, Integer> pagesCountMap = pageDao.countPagesGroupedBySite(currentBatch);
            Map<Integer, Integer> lemmasCountMap = lemmaService.countLemmasGroupedBySite(currentBatch);

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


    public DetailedStatisticsItem getDetailed(Site site, Map<Integer, Integer> pagesCountMap,
                                              Map<Integer, Integer> lemmasCountMap) {
        logger.debug("Формирование детализированной статистики для сайта: {}", site.getUrl());

        Integer pages = pagesCountMap.getOrDefault(site.getId(), 0);
        Integer lemmas = lemmasCountMap.getOrDefault(site.getId(), 0);

        DetailedStatisticsItem detailedItem = new DetailedStatisticsItem(
                site.getUrl(), site.getName(), site.getStatus(),
                getStatusTime(site), site.getLastError(), pages, lemmas);
        logger.debug("Детализированная статистика сформирована: {}", detailedItem);
        return detailedItem;
    }


    private int getStatusTime(Site site) {
        if (site.getStatusTime() != null) {
            ZonedDateTime zdt = site.getStatusTime()
                    .atZone(ZoneId.systemDefault());
            return Math.toIntExact(zdt.toInstant().toEpochMilli());
        }
        return 0;
    }

    private StatisticsReport getTotal() {
        Integer siteNumber = siteService.countSites();
        Integer pageNumber = pageService.countPages();
        Integer lemmaNumber = lemmaDao.countLemmas();

        boolean isIndexing = siteService.isAnySiteIndexing();

        logger.debug("Общая статистика: " +
                        "siteNumber={}, pageNumber={}, " +
                        "lemmaNumber={}, isIndexing={}",
                siteNumber, pageNumber, lemmaNumber, isIndexing);

        return new StatisticsReport(siteNumber, pageNumber,
                lemmaNumber, isIndexing);
    }
}
