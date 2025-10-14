package com.example.searchengine.indexing;

import com.example.searchengine.services.SiteValidationService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.exceptions.InvalidSiteException;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;


@Service
@Transactional
public class SiteIndexer {

    private final IndexService indexService;
    private final DBSaver dbSaver;
    private final UrlRecursiveParser urlRecursiveParser;

    private static final Logger logger =
            LoggerFactory.getLogger(SiteIndexer.class);


    @Autowired
    private SiteValidationService validationService;

    @Autowired
    public SiteIndexer(
            IndexService indexService,
            DBSaver dbSaver,
            UrlRecursiveParser urlRecursiveParser
    ) {
        this.indexService = indexService;
        this.dbSaver = dbSaver;
        this.urlRecursiveParser = urlRecursiveParser;
    }


    public void indexMainPage(String siteUrl, Set<String> visitedLinks)
            throws IOException, InvalidSiteException {

        logger.info("Индексируем главную страницу сайта: {}", siteUrl);
        try {
            indexService.indexPage(siteUrl);
            visitedLinks.add(siteUrl);
            dbSaver.addPagesToDatabase(siteUrl);

            logger.info("Главная страница успешно проиндексирована: {}", siteUrl);
        } catch (IOException | InvalidSiteException e) {
            logger.error(
                    "Ошибка при индексации главной страницы: " +
                            "{}", e.getMessage(), e);
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }


    public List<Boolean> areSitesAvailable(List<String> urls) throws InterruptedException {
        int maxThreads = Math.min(Runtime.getRuntime().availableProcessors(), urls.size());
        ExecutorService executor = Executors.newFixedThreadPool(maxThreads);
        List<Future<Boolean>> futures = new ArrayList<>(urls.size());

        for (String url : urls) {
            Callable<Boolean> task = () -> validationService.checkSiteAvailability(url);
            Future<Boolean> future = executor.submit(task);
            futures.add(future);
        }

        List<Boolean> results = new ArrayList<>(urls.size());
        for (Future<Boolean> future : futures) {
            try {
                results.add(future.get());
            } catch (ExecutionException e) {
                logger.error("Во время выполнения задачи возникло исключение: {}",
                        e.getCause().getMessage());
                results.add(false);
            }
        }

        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            logger.warn("Некоторые задачи продолжали выполняться после отключения пула.");
        }
        return results;
    }


    public void indexAdditionalPages(String siteUrl, Set<String> visitedLinks)
            throws IOException, InvalidSiteException {

        logger.info("Индексируем дополнительные страницы сайта: {}", siteUrl);

        try {
            Set<String> parsedLinks = urlRecursiveParser.startParsing(siteUrl);

            for (String page : parsedLinks) {
                if (!visitedLinks.contains(page)) {
                    logger.info("Индексируем дополнительную страницу: {}", page);

                    indexService.indexPage(page);
                    dbSaver.addPagesToDatabase(page);
                    visitedLinks.add(page);
                }
            }

            logger.info("Дополнительные страницы успешно проиндексированы");
        } catch (IOException | InvalidSiteException e) {
            logger.error(
                    "Ошибка при индексации дополнительных страниц: {}",
                    e.getMessage(), e);
            throw e;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
