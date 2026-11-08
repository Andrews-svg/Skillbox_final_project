package com.example.searchengine.services.crawler;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.services.indexing.IndexingState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.ForkJoinPool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CrawlerService - crawlSite/indexPage/stop* contract")
class CrawlerServiceTest {

    @Mock private SiteService siteService;
    @Mock private SiteCrawler siteCrawler;
    @Mock private PageProcessor pageProcessor;

    private IndexingState indexingState;
    private CrawlerConfig crawlerConfig;
    private CrawlerService crawlerService;

    @BeforeEach
    void setUp() {
        indexingState = new IndexingState();
        crawlerConfig = new CrawlerConfig();
        crawlerService = new CrawlerService(
                siteService, siteCrawler, pageProcessor, indexingState, crawlerConfig);
    }

    // ===== crawlSite: 3 =====

    @Test
    @DisplayName("crawlSite: inactive -> siteCrawler untouched, isIndexing false")
    void crawlSite_inactive_noop() {
        Site site = site(1L, "https://site.example");

        crawlerService.crawlSite(site);

        verifyNoInteractions(siteCrawler);
        assertThat(crawlerService.isIndexing()).isFalse();
    }

    @Test
    @DisplayName("crawlSite: active, crawler succeeds -> siteCrawler.crawl invoked once")
    void crawlSite_active_invokesCrawler() {
        indexingState.setActive(true);
        Site site = site(1L, "https://site.example");

        crawlerService.crawlSite(site);

        verify(siteCrawler).crawl(eq(site), any(ForkJoinPool.class));
        verify(siteService, never()).updateStatusWithError(any(), any());
    }

    @Test
    @DisplayName("crawlSite: active, crawler throws -> updateStatusWithError called")
    void crawlSite_crawlerThrows_updatesStatusWithError() {
        indexingState.setActive(true);
        Site site = site(1L, "https://site.example");
        doThrow(new RuntimeException("crawl boom"))
                .when(siteCrawler).crawl(eq(site), any(ForkJoinPool.class));

        crawlerService.crawlSite(site);

        verify(siteService).updateStatusWithError(eq(site), contains("Ошибка обхода"));
    }

    // ===== indexPage: 3 =====

    @Test
    @DisplayName("indexPage: processor returns present -> true")
    void indexPage_present_returnsTrue() {
        Site site = site(1L, "https://site.example");
        Page page = new Page("/x", 200, "<html></html>", site);
        when(pageProcessor.processPage(site, "https://site.example/x"))
                .thenReturn(Optional.of(new ProcessedPage(page, null)));

        boolean result = crawlerService.indexPage(site, "https://site.example/x");

        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("indexPage: processor returns empty -> false")
    void indexPage_empty_returnsFalse() {
        Site site = site(1L, "https://site.example");
        when(pageProcessor.processPage(site, "https://site.example/x"))
                .thenReturn(Optional.empty());

        boolean result = crawlerService.indexPage(site, "https://site.example/x");

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("indexPage: processor throws -> false (not propagated)")
    void indexPage_processorThrows_returnsFalse() {
        Site site = site(1L, "https://site.example");
        when(pageProcessor.processPage(site, "https://site.example/x"))
                .thenThrow(new RuntimeException("processor boom"));

        boolean result = crawlerService.indexPage(site, "https://site.example/x");

        assertThat(result).isFalse();
    }

    // ===== stopAllCrawling: 1 =====

    @Test
    @DisplayName("stopAllCrawling: delegates to siteCrawler and deactivates state")
    void stopAllCrawling_delegatesAndDeactivates() {
        indexingState.setActive(true);

        crawlerService.stopAllCrawling();

        verify(siteCrawler).stopAllCrawling();
        assertThat(indexingState.isActive()).isFalse();
    }

    // ===== stopCrawling(siteId): 1 =====

    @Test
    @DisplayName("stopCrawling: INDEXING site -> delegated, status updated with user-stop message")
    void stopCrawling_indexingSite_updatesStatus() {
        Site site = site(7L, "https://site.example");
        site.setStatus(Status.INDEXING);
        when(siteService.findById(7L)).thenReturn(Optional.of(site));

        crawlerService.stopCrawling(7L);

        verify(siteCrawler).stopCrawling(7L);
        verify(siteService).updateStatusWithError(eq(site), contains("Обход остановлен"));
    }

    // ===== helpers =====

    private static Site site(Long id, String url) {
        Site s = new Site("Site", url);
        s.setId(id);
        s.setStatus(Status.INDEXING);
        return s;
    }
}