package com.example.searchengine.services;

import com.example.searchengine.config.Site;
import com.example.searchengine.exceptions.InvalidPageException;
import jakarta.persistence.EntityManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Service
public class PageService {

    private final PageRepository pageRepository;
    private final EntityManager entityManager;

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);

    @Autowired
    public PageService(PageRepository pageRepository,
                       EntityManager entityManager) {
        this.pageRepository = pageRepository;
        this.entityManager = entityManager;
    }



    @Transactional
    public void createPage(Page page) {
        validatePage(page);
        pageRepository.save(page);
    }


    @Transactional
    public void updatePage(Page updatedPage) {
        validatePage(updatedPage);
        pageRepository.save(updatedPage);
    }


    @Transactional
    public void savePage(Page page) throws InvalidPageException {
        validatePage(page);
        pageRepository.save(page);
    }


    @Transactional
    public void saveAll(List<Page> pages) throws InvalidPageException {
        int batchSize = 100;
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            validatePage(page);
            pageRepository.save(page);
            if (i % batchSize == 0 && i > 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }


    @Transactional
    public void deletePage(Long pageId) {
        pageRepository.deleteById(pageId);
    }


    @Transactional(readOnly = true)
    public Optional<Page> findByPath(String path) {
        return pageRepository.findByPath(path);
    }


    @Transactional(readOnly = true)
    public Optional<Page> findPageById(Long pageId) {
        return pageRepository.findById(pageId);
    }


    @Transactional(readOnly = true)
    public Optional<Page> findPageByPath(String path) {
        return pageRepository.findByPath(path);
    }


    @Cacheable(value="pageCount")
    @Transactional(readOnly = true)
    public long getTotalPages() {
        return pageRepository.count();
    }


    public Map<Long, Long> countPagesGroupedBySite(List<Site> sites) {
        int maxBatchSize = 1000;
        List<List<Site>> batches = partitionSites(sites, maxBatchSize);
        List<Map<Long, Long>> partialResults = new ArrayList<>();
        parallelProcess(batches, batch -> {
            List<Page> filteredPages = pageRepository.findAllBySiteIn(batch);
            Map<Long, Long> result = filteredPages.stream()
                    .collect(Collectors.groupingBy(page ->
                            page.getSite().getId(), Collectors.counting()));
            synchronized(partialResults) {
                partialResults.add(result);
            }
        });
        return combineMaps(partialResults);
    }


    private List<List<Site>> partitionSites(List<Site> sites, int batchSize) {
        List<List<Site>> partitions = new ArrayList<>();
        for (int i = 0; i < sites.size(); i += batchSize) {
            partitions.add(sites.subList(i, Math.min(i + batchSize, sites.size())));
        }
        return partitions;
    }


    private void parallelProcess(List<List<Site>> batches, Consumer<List<Site>> processor) {
        try (ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors())) {
            batches.forEach(batch -> pool.submit(() -> processor.accept(batch)));
            pool.shutdown();
            try {
                boolean terminated = pool.awaitTermination(1, TimeUnit.MINUTES);
                if (!terminated) {
                    throw new TimeoutException("Timeout occurred while waiting for tasks to complete");
                }
            } catch (InterruptedException | TimeoutException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Processing interrupted", e);
            }
        }
    }


    private Map<Long, Long> combineMaps(List<Map<Long, Long>> maps) {
        return maps.stream()
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        Long::sum));
    }


    private void validatePage(Page page) {
        if (page.getSite() == null) {
            throw new IllegalArgumentException("Ссылка на сайт обязательна!");
        }
        if (page.getPath() == null || page.getPath().trim().isEmpty()) {
            throw new IllegalArgumentException("Адрес страницы обязателен!");
        }
        if (page.getContent() == null || page.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Контент страницы обязателен!");
        }
    }
}