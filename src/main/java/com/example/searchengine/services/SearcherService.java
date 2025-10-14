package com.example.searchengine.services;

import com.example.searchengine.dao.PageDao;
import com.example.searchengine.dto.statistics.Data;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.repository.SiteRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;


@Service
public class SearcherService {

    private static final Logger logger =
            LoggerFactory.getLogger(SearcherService.class);

    private final PageDao pageDao;
    private final SiteRepository siteRepository;
    private final EntityManager entityManager;

    @Autowired
    public SearcherService(PageDao pageDao,
                           SiteRepository siteRepository,
                           EntityManager entityManager) {
        this.pageDao = pageDao;
        this.siteRepository = siteRepository;
        this.entityManager = entityManager;
    }


    public List<Site> findByPartialUrl(String partialUrl) {
        return siteRepository.findByUrlContaining(partialUrl);
    }


    public Site findByName(String url) {
        return executeWithLogging(() -> {
            TypedQuery<Site> query = entityManager.createQuery(
                    "FROM Site WHERE url LIKE :url", Site.class);
            query.setParameter("url", url);
            Site site = query.getSingleResult();
            logger.info("Site found by name '{}': {}", url, site);
            return site;
        }, "Failed to find site by name");
    }


    public Site findByExactName(String url) {
        return executeWithLogging(() -> {
            TypedQuery<Site> query = entityManager.createQuery(
                    "FROM Site WHERE url = :url", Site.class);
            query.setParameter("url", url);
            Site site = query.getSingleResult();
            logger.info("Site found by exact name '{}': {}", url, site);
            return site;
        }, "Failed to find site by exact name");
    }


    public Boolean checkIfSiteExistsByExactMatch(String name) {
        return executeWithLogging(() -> {
            TypedQuery<Site> query = entityManager.createQuery(
                    "FROM Site WHERE url = :url", Site.class);
            query.setParameter("url", name);
            boolean exists = !query.getResultList().isEmpty();
            logger.info("Site existence check for exact match '{}': " +
                    "{}", name, exists);
            return exists;
        }, "Failed to check if site exists by exact match");
    }


    public Boolean checkIfSiteExists(String name) {
        return executeWithLogging(() -> {
            TypedQuery<Site> query = entityManager.createQuery(
                    "FROM Site WHERE url LIKE :url", Site.class);
            query.setParameter("url", name);
            boolean exists = !query.getResultList().isEmpty();
            logger.info("Site existence check for '{}': {}", name, exists);
            return exists;
        }, "Failed to check if site exists");
    }


    public boolean checkIfSiteWithNameExists(String siteUrl) {
        return siteRepository.existsByUrl(siteUrl);
    }

    public ArrayList<Data> getDataFromSearchInput(
            String query, String site, int offset, int limit) {
        if (query == null || query.trim().isEmpty()) {
            logger.warn("Передан пустой или неверный запрос.");
            return new ArrayList<>();
        }
        List<Page> pages = site !=
                null ? pageDao.getPagesBySiteUrl(site) : pageDao.getAllPages();

        List<Page> matchingPages = pages.stream()
                .filter(page ->
                        page.getContent().
                                toLowerCase().contains(query.toLowerCase()))
                .toList();

        ArrayList<Data> results = new ArrayList<>();
        for (Page page : matchingPages) {
            results.add(new Data.Builder()
                    .title(page.getTitle())
                    .uri(page.getUrl())
                    .snippet(getPageSnippet(page.getContent(), query))
                    .relevance(calculateRelevance(query, page))
                    .build());
        }

        return paginateResults(results, offset, limit);
    }


    private ArrayList<Data> paginateResults(
            ArrayList<Data> allResults, int offset, int limit) {
        int end = Math.min(offset + limit, allResults.size());
        if (offset >= end) {
            return new ArrayList<>();
        }
        return new ArrayList<>(allResults.subList(offset, end));
    }


    private float calculateRelevance(String query, Page page) {
        String normalizedQuery = query.toLowerCase();
        String content = page.getContent().toLowerCase();
        String title = page.getTitle() !=
                null ? page.getTitle().toLowerCase() : "";

        int contentMatches = countMatches(content, normalizedQuery);
        int titleMatches = countMatches(title, normalizedQuery);

        float relevance = (contentMatches + titleMatches) /
                (float) normalizedQuery.split("\\s+").length;
        return Math.min(Math.max(relevance, 0), 1);
    }


    private int countMatches(String text, String query) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(query, index)) != -1) {
            count++;
            index += query.length();
        }
        return count;
    }


    public Long getTotalCount(String query, String site) {
        if (site != null) {
            return pageDao.countByContentContainingAndSiteUrl(query, site);
        } else {
            return pageDao.countByContentContaining(query);
        }
    }


    private String getPageSnippet(String content, String query) {
        int pos = content.toLowerCase().indexOf(query.toLowerCase());
        if (pos > -1) {
            int start = Math.max(pos - 100, 0);
            int end = Math.min(pos + query.length() + 100, content.length());
            return content.substring(start, end);
        }
        return content.substring(0, Math.min(200, content.length())) + "...";
    }

    private <T> T executeWithLogging(Supplier<T> action, String errorMessage) {
        try {
            return action.get();
        } catch (Exception e) {
            logger.error("{}: {}", errorMessage, e.getMessage());
            throw new RuntimeException(errorMessage, e);
        }
    }

}
