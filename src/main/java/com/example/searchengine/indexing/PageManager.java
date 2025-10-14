package com.example.searchengine.indexing;

import com.example.searchengine.services.SearcherService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.dao.LemmaDao;
import com.example.searchengine.dao.PageDao;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.repository.IndexRepository;
import com.example.searchengine.services.IndexingHistoryService;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.SiteService;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@Transactional
public class PageManager {

    private static final Logger logger =
            LoggerFactory.getLogger(PageManager.class);
    private static final String NEITHER_FOUND = "NEITHER_FOUND";
    private static final String PAGE_NOT_FOUND = "PAGE_NOT_FOUND";
    private static final String LEMMA_NOT_FOUND = "LEMMA_NOT_FOUND";
    private static final String BOTH_INDEXED = "BOTH_INDEXED";

    private final IndexRepository indexRepository;
    private final LemmaDao lemmaDao;
    private final PageDao pageDao;
    private final PageService pageService;
    private final IndexService indexService;
    private final SearcherService searcherService;
    private final IndexingHistoryService indexingHistoryService;
    private final SiteService siteService;
    private final SiteManager siteManager;
    private final IndexingService indexingService;

    private Long currentIndexingLemmaId;
    private List<Long> indexedLemmaIds;

    @Autowired
    public PageManager(IndexRepository indexRepository,
                       LemmaDao lemmaDao, PageDao pageDao,
                       PageService pageService,
                       IndexService indexService, SearcherService searcherService,
                       SiteManager siteManager,
                       IndexingHistoryService indexingHistoryService,
                       SiteService siteService,
                       IndexingService indexingService) {
        this.indexRepository = indexRepository;
        this.lemmaDao = lemmaDao;
        this.pageDao = pageDao;
        this.pageService = pageService;
        this.indexService = indexService;
        this.searcherService = searcherService;
        this.indexingHistoryService = indexingHistoryService;
        this.siteService = siteService;
        this.siteManager = siteManager;
        this.indexingService = indexingService;
    }

    public String getIndexingStatus(Long pageId, Long lemmaId) {
        boolean exists = indexRepository.existsByPageIdAndLemmaId(pageId, lemmaId);
        String status = exists ? "Indexed" : "Not Found";
        logger.info("Статус индексации getIndexingStatus " +
                "для страницы ID {} и леммы ID {}: {}", pageId, lemmaId, status);
        return status;
    }


    public String getIndexingStatusById(Long id, boolean isLemma) {
        if (isLemma) {
            return lemmaDao.findById(id)
                    .map(lemma -> {
                        String status = lemma.getStatus() != null ?
                                lemma.getStatus().toString() : "Status not available";
                        logger.info("Статус леммы ID {}: {}", id, status);
                        return status;
                    })
                    .orElseGet(() -> {
                        logger.warn("Лемма не найдена для ID: {}", id);
                        return "Lemma Not Found";
                    });
        } else {
            return pageService.findById(id)
                    .map(page -> {
                        String status = page.getStatus() != null ?
                                page.getStatus().toString() : "Status not available";
                        logger.info("Статус страницы ID {}: {}", id, status);
                        return status;
                    })
                    .orElseGet(() -> {
                        logger.warn("Страница не найдена для ID: {}", id);
                        return "Page Not Found";
                    });
        }
    }


    public String getIndexingStatusForBoth(Long pageId, Long lemmaId) {
        boolean pageExists = indexRepository.existsIndexByPageId(pageId);;
        boolean lemmaExists = indexRepository.existsByLemmaId(lemmaId);

        String result;
        if (!pageExists && !lemmaExists) {
            result = NEITHER_FOUND;
        } else if (!pageExists) {
            result = PAGE_NOT_FOUND;
        } else if (!lemmaExists) {
            result = LEMMA_NOT_FOUND;
        } else {
            result = BOTH_INDEXED;
        }

        logger.info("Статус индексации для страницы ID {} " +
                "и леммы ID {}: {}", pageId, lemmaId, result);
        return result;
    }


    public Status getIndexingStatusForLemma(Long lemmaId) {
        Optional<Lemma> lemmaOptional = lemmaDao.findById(lemmaId);
        if (lemmaOptional.isEmpty()) {
            logger.warn("Лемма " +
                    "getIndexingStatusForLemma не найдена для ID: {}", lemmaId);
            return Status.NOT_FOUND;
        }
        synchronized (this) {
            if (indexedLemmaIds.contains(lemmaId)) {
                logger.info("Лемма ID {} уже индексирована.", lemmaId);
                return Status.INDEXED;
            } else if (!indexingService.isIndexing() &&
                    currentIndexingLemmaId != null) {
                logger.warn("Индексация леммы ID {} завершилась неудачно.", lemmaId);
                return Status.FAILED;
            } else {
                logger.info("Лемма ID {} ожидает индексации.", lemmaId);
                return Status.PENDING;
            }
        }
    }

    public void removePageAndIndexes(String url) {
        URI parsedUrl;
        try {
            parsedUrl = new URI(url);
        } catch (URISyntaxException e) {
            logger.error("Ошибка разбора URL: {}", url, e);
            return;
        }
        String path = parsedUrl.getPath();
        Optional<Page> optionalPage = pageDao.findByName(path);
        optionalPage.ifPresentOrElse(
                pageDao::delete,
                () -> logger.warn("Страница не найдена для удаления: {}", url)
        );
    }

    
    public void removeSiteWithDependencies(String siteUrl) {
        if (!searcherService.checkIfSiteWithNameExists(siteUrl)) {
            logger.warn("Сайт не найден для удаления: {}", siteUrl);
            return;
        }
        deleteSiteAndRelatedDataByExactMatch(siteUrl);
    }

    private void deleteSiteAndRelatedData(Site site) {
        if (site == null) {
            logger.warn("Сайт не найден для удаления.");
            return;
        }
        deletePagesAndLemmas(site);
        siteService.delete(site);
        logger.info("Сайт и связанные данные успешно удалены: {}", site.getName());
    }

    private void deleteSiteAndRelatedDataByExactMatch(String searchURL) {
        Optional<Site> optionalSite = siteService.findByUrl(searchURL);
        if (optionalSite.isPresent()) {
            Site site = optionalSite.get();
            deleteSiteAndRelatedData(site);
        } else {
            logger.warn("Сайт с URL {} не найден и не будет удалён", searchURL);
        }
    }

    private void deletePagesAndLemmas(Site site) {

        List<Page> pagesList = pageDao.findAllBySite(site);

        List<Lemma> lemmaList = lemmaDao.findAllBySite(site);

        UUID sessionId = indexingHistoryService.startIndexingSession();

        pagesList.forEach(page -> {
            indexService.deleteByPageId(page.getId(), sessionId);
            pageService.deletePage(page);
            logger.info("Страница с ID {} успешно удалена.", page.getId());
        });

        lemmaList.forEach(lemma -> {
            lemmaDao.delete(lemma);
            logger.info("Лемма с ID {} успешно удалена.", lemma.getId());
        });

        logger.info("Страницы и леммы успешно удалены для сайта: {}", site.getName());
    }
}
