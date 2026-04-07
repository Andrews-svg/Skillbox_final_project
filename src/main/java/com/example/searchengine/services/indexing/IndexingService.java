package com.example.searchengine.services.indexing;

import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.*;
import com.example.searchengine.services.crawler.CrawlerService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class IndexingService {

    private static final Logger logger = LoggerFactory.getLogger(IndexingService.class);

    private final SiteService siteService;
    private final PageService pageService;
    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final CrawlerService crawlerService;
    private final SitesList sitesList;
    private final IndexingState indexingState;


    private final ExecutorService indexingExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        t.setName("indexing-starter");
        return t;
    });

    public IndexingService(SiteService siteService,
                           PageService pageService,
                           LemmaService lemmaService,
                           IndexService indexService,
                           CrawlerService crawlerService,
                           SitesList sitesList, IndexingState indexingState) {
        this.siteService = siteService;
        this.pageService = pageService;
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.crawlerService = crawlerService;
        this.sitesList = sitesList;
        this.indexingState = indexingState;
    }


    @PreDestroy
    public void destroy() {
        logger.info("=== ЗАВЕРШЕНИЕ РАБОТЫ, ОСТАНАВЛИВАЕМ ВСЕ ПОТОКЫ ===");
        if (indexingState.isActive()) {
            try {
                stopIndexing();
            } catch (Exception e) {
                logger.warn("Ошибка при остановке индексации: {}", e.getMessage());
            }
        }
        indexingExecutor.shutdownNow();
        try {
            if (!indexingExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("Executor не завершился за 10 секунд");
            }
        } catch (InterruptedException e) {
            logger.warn("Прерывание при ожидании завершения executor");
            Thread.currentThread().interrupt();
        }
        logger.info("=== ВСЕ ПОТОКИ ОСТАНОВЛЕНЫ ===");
    }



    public synchronized void startFullIndexing() {
        if (indexingState.isActive()) {
            throw new IllegalStateException("Индексация уже запущена");
        }
        if (indexingExecutor.isShutdown()) {
            throw new IllegalStateException("Executor сервис остановлен");
        }
        indexingState.setActive(true);
        logger.info("=== ЗАПУСК ПОЛНОЙ ИНДЕКСАЦИИ ===");
        List<SitesList.SiteConfig> configs = sitesList.getSites();
        if (configs.isEmpty()) {
            indexingState.setActive(false);
            throw new IllegalStateException("В конфигурации нет сайтов для индексации");
        }
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalSites = configs.size();
        for (SitesList.SiteConfig config : configs) {
            indexingExecutor.submit(() -> {
                if (!indexingState.isActive()) {
                    logger.info("Индексация прервана для сайта: {}", config.getUrl());
                    return;
                }
                try {
                    indexSite(config);
                } catch (Exception e) {
                    logger.error("Ошибка при индексации сайта {}: {}",
                            config.getUrl(), e.getMessage(), e);
                } finally {
                    if (completedCount.incrementAndGet() == totalSites) {
                        indexingState.setActive(false);
                        logger.info("=== ВСЕ САЙТЫ ЗАВЕРШИЛИ ИНДЕКСАЦИЮ ===");
                    }
                }
            });
        }
        logger.info("Запущена индексация {} сайтов", configs.size());
    }


    private void indexSite(SitesList.SiteConfig config) {
        logger.info("🔥🔥🔥 indexSite() ВЫЗВАН для сайта: {}", config.getUrl());
        logger.info("Параметры: url={}, name={}", config.getUrl(), config.getName());
        Site site = null;
        try {
            logger.info("1. Поиск сайта в БД: {}", config.getUrl());
            site = siteService.findByUrl(config.getUrl())
                    .orElseGet(() -> {
                        logger.info("   Сайт не найден, создаем новый: {}", config.getUrl());
                        return siteService.createNewSite(config.getUrl(), config.getName());
                    });
            logger.info("   Сайт получен: id={}, status={}, name={}",
                    site.getId(), site.getStatus(), site.getName());
            logger.info("2. Очистка старых данных для сайта: {}", config.getUrl());
            clearSiteData(site);
            logger.info("3. Установка статуса INDEXING для сайта: {}", config.getUrl());
            siteService.updateStatus(site, Status.INDEXING);
            logger.info("4. ВЫЗОВ crawlerService.crawlSite() для {}", site.getUrl());
            logger.info("   Время перед вызовом: {}", System.currentTimeMillis());
            try {
                crawlerService.crawlSite(site);
                logger.info("5. crawlerService.crawlSite() ЗАВЕРШЕН для {}", site.getUrl());
            } catch (Exception e) {
                logger.error("💥 ИСКЛЮЧЕНИЕ в crawlerService.crawlSite() для {}:", site.getUrl(), e);
                throw e;
            }
            logger.info("6. Время после вызова: {}", System.currentTimeMillis());
            if (indexingState.isActive()) {
                long pageCount = pageService.countBySite(site);
                logger.info("7. Индексация активна. Найдено страниц: {}", pageCount);
                siteService.updateStatus(site, Status.INDEXED);
                logger.info("✅ Сайт успешно проиндексирован: {} ({} страниц)",
                        site.getUrl(), pageCount);
            } else {
                logger.info("⛔ Индексация сайта {} прервана пользователем", site.getUrl());
            }
        } catch (Exception e) {
            logger.error("❌❌❌ ИСКЛЮЧЕНИЕ в indexSite для сайта {}: {}",
                    config.getUrl(), e.getMessage());
            logger.error("Тип исключения: {}", e.getClass().getName());
            logger.error("Стек ошибки:", e);
            if (site != null) {
                try {
                    logger.info("Попытка обновить статус сайта с ошибкой");
                    siteService.updateStatusWithError(site,
                            "Ошибка индексации: " + e.getMessage());
                } catch (Exception ex) {
                    logger.warn("Не удалось обновить статус ошибки: {}", ex.getMessage());
                }
            }
        } finally {
            logger.info("8. Finally блок для сайта: {}", config.getUrl());
            logger.info("========== indexSite() ЗАВЕРШЕН для {} ==========", config.getUrl());
        }
    }


    private void clearSiteData(Site site) {
        try {
            logger.debug("Очистка данных сайта: {}", site.getUrl());
            pageService.deleteAllBySite(site);
            lemmaService.deleteAllBySite(site);
            indexService.deleteAllBySite(site);
        } catch (Exception e) {
            logger.warn("Ошибка при очистке данных сайта {}: {}",
                    site.getUrl(), e.getMessage());
        }
    }


    public boolean indexPage(String url) {
        try {
            logger.info("📥 Ручная индексация страницы: {}", url);
            SitesList.SiteConfig config = sitesList.getSites().stream()
                    .filter(s -> url.startsWith(s.getUrl()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Страница не принадлежит ни одному из сайтов в конфигурации"));
            Site site = siteService.findByUrl(config.getUrl())
                    .orElseGet(() ->
                            siteService.createNewSite(config.getUrl(), config.getName()));
            String path = extractPath(url, site.getUrl());
            pageService.findByPathAndSite(path, site).ifPresent(page -> {
                logger.info("Удаляем старую версию страницы: {}", path);
                try {
                    indexService.deleteByPage(page);
                    lemmaService.decrementAllForPage(page);
                    pageService.delete(page);
                } catch (Exception e) {
                    logger.warn("Ошибка при удалении старой версии: {}", e.getMessage());
                }
            });
            long startTime = System.currentTimeMillis();
            boolean success = crawlerService.indexPage(site, url);
            long duration = System.currentTimeMillis() - startTime;
            if (success) {
                logger.info("✅ Страница успешно проиндексирована за {} мс: {}", duration, url);
            } else {
                logger.warn("❌ Не удалось проиндексировать страницу: {}", url);
            }
            return success;
        } catch (IllegalArgumentException e) {
            logger.error("Ошибка: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            logger.error("Ошибка индексации страницы {}: {}", url, e.getMessage(), e);
            return false;
        }
    }


    private String extractPath(String fullUrl, String baseUrl) {
        String path = fullUrl.substring(baseUrl.length());
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }
        if (path.contains("#")) {
            path = path.substring(0, path.indexOf("#"));
        }
        return path;
    }


    public synchronized void stopIndexing() {
        if (!indexingState.isActive()) {
            throw new IllegalStateException("Индексация не запущена");
        }
        logger.info("=== ОСТАНОВКА ИНДЕКСАЦИИ ПО ЗАПРОСУ ПОЛЬЗОВАТЕЛЯ ===");
        indexingState.setActive(false);
        try {
            crawlerService.stopAllCrawling();
        } catch (Exception e) {
            logger.warn("Ошибка при остановке краулера: {}", e.getMessage());
        }
        try {
            indexingExecutor.shutdownNow();
        } catch (Exception e) {
            logger.warn("Ошибка при остановке executor: {}", e.getMessage());
        }
        try {
            siteService.findAll().stream()
                    .filter(site -> site.getStatus() == Status.INDEXING)
                    .forEach(site -> {
                        siteService.updateStatusWithError(
                                site, "Индексация остановлена пользователем");
                    });
        } catch (Exception e) {
            logger.warn("Ошибка при обновлении статусов: {}", e.getMessage());
        }
        logger.info("✅ Индексация остановлена");
    }


    public void onPageIndexed(Site site) {
        try {
            siteService.updateStatusTime(site);
        } catch (Exception e) {
            logger.warn("Ошибка при обновлении времени: {}", e.getMessage());
        }
    }
}