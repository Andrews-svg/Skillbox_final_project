package com.example.searchengine.indexing;

import com.example.searchengine.exceptions.IndexingStatusFetchException;
import com.example.searchengine.models.Page;
import org.jsoup.nodes.Document;
import com.example.searchengine.config.Site;
import com.example.searchengine.models.Status;

import java.io.IOException;
import java.util.*;

public interface IndexingService {


        void init();

        void ensureInitialData();

        void updateSiteStatus(int siteId, Status newStatus, String errorMessage);

        void persistLink(String link);

        String parsePathFromLink(String link);

        Site resolveSiteFromLink(String link);

        String extractHostFromLink(String link);

        boolean validateLink(String link);

        void batchSavePages(List<Page> pages);

        void prepareIndexing(int siteId) throws Exception;

        void crawlAndIndex(Site site) throws Exception;

        void processSinglePage(String currentUrl,
                               Site site, List<Page> pagesBatch,
                               Queue<String> urlsToVisit) throws IOException;

        Page createPageFromDocument(Site site, String path, Document doc);

        void addLinksFromPage(Site site, Document doc, Queue<String> urlsToVisit);

        void finishIndexing(int siteId, boolean isSuccess);

        void indexSite(int siteId) throws Exception;

        void startFullIndexing();

        Site generateNewSite(String url);

        void startIndexing(Integer id, boolean isLemma);

        void handleError(Integer id, Throwable t);

        void stopIndexing();

        void processIndexingTask(Integer id);

        String getSiteUrlForId(Integer id);

        boolean canStartIndexing(Integer id);

        void deleteEntireSiteData(Site site);

        boolean isIndexingInProgress();

        void setIndexingStopped();

        String getStatus(Integer id) throws IndexingStatusFetchException;

        void logInfo(String message, Object... args);

}