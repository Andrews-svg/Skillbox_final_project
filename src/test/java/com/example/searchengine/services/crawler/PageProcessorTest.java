package com.example.searchengine.services.crawler;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.indexing.IndexService;
import com.example.searchengine.services.LemmaService;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.indexing.IndexingState;
import com.example.searchengine.services.indexing.PageIndexingService;
import com.example.searchengine.utils.Lemmatizer;
import com.example.searchengine.utils.UrlFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PageProcessor - processPage contract (browser path)")
class PageProcessorTest {

    @Mock private PageService pageService;
    @Mock private LemmaService lemmaService;
    @Mock private IndexService indexService;
    @Mock private PageIndexingService pageIndexingService;
    @Mock private Lemmatizer lemmatizer;
    @Mock private CrawlerConfig crawlerConfig;
    @Mock private UrlFilter urlFilter;
    @Mock private IndexingState indexingState;
    @Mock private SeleniumFetcher seleniumFetcher;
    @Mock private WatchdogService watchdogService;

    private PageProcessor pageProcessor;

    @BeforeEach
    void setUp() {
        pageProcessor = new PageProcessor(
                pageService, lemmaService, indexService, pageIndexingService,
                lemmatizer, crawlerConfig, urlFilter, indexingState,
                seleniumFetcher, watchdogService);
    }

    // ===== processPage: 6 =====

    @Test
    @DisplayName("processPage: success browser path -> saved, indexed, counters incremented")
    void processPage_success() {
        Site site = site(1L, "https://site.example");
        String pageUrl = "https://site.example/page1";
        Document doc = docWithTitleAndText("Test", "a".repeat(150));

        when(indexingState.isActive()).thenReturn(true);
        when(urlFilter.shouldIndex(pageUrl)).thenReturn(true);
        when(crawlerConfig.getRandomDelay()).thenReturn(0);
        when(crawlerConfig.getRetryCount()).thenReturn(1);
        when(seleniumFetcher.shouldUseBrowser(pageUrl)).thenReturn(true);
        when(seleniumFetcher.fetchWithBrowser(pageUrl)).thenReturn(doc);
        when(urlFilter.normalizePath(pageUrl, site.getUrl())).thenReturn("/page1");
        when(pageService.existsByPathAndSite("/page1", site)).thenReturn(false);
        when(lemmatizer.getLemmasFrequency("a".repeat(150))).thenReturn(new HashMap<>(Map.of("a", 1)));
        when(lemmatizer.getLemmasFrequency("Test")).thenReturn(Map.of("t", 2));
        when(pageService.save(any(Page.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pageIndexingService.savePageLemmas(any(Page.class), eq(site), any())).thenReturn(5);

        Optional<ProcessedPage> result = pageProcessor.processPage(site, pageUrl);

        assertThat(result).isPresent();
        verify(watchdogService).notifyActivity();
        verify(pageService).save(any(Page.class));
        verify(pageIndexingService).savePageLemmas(any(Page.class), eq(site), any());
        verify(indexingState).incrementPages(1L, 1);
        verify(indexingState).incrementLemmas(1L, 5);
    }

    @Test
    @DisplayName("processPage: inactive at start -> empty, no fetch, no save")
    void processPage_inactive() {
        Site site = site(1L, "https://site.example");
        when(indexingState.isActive()).thenReturn(false);

        Optional<ProcessedPage> result = pageProcessor.processPage(site, "https://site.example/p");

        assertThat(result).isEmpty();
        verify(watchdogService).notifyActivity();
        verifyNoInteractions(seleniumFetcher, pageService, lemmaService, indexService, pageIndexingService);
    }

    @Test
    @DisplayName("processPage: shouldIndex=false -> empty, no fetch")
    void processPage_filtered() {
        Site site = site(1L, "https://site.example");
        String pageUrl = "https://site.example/p";
        when(indexingState.isActive()).thenReturn(true);
        when(urlFilter.shouldIndex(pageUrl)).thenReturn(false);

        Optional<ProcessedPage> result = pageProcessor.processPage(site, pageUrl);

        assertThat(result).isEmpty();
        verifyNoInteractions(seleniumFetcher, pageService, lemmaService, indexService, pageIndexingService);
    }

    @Test
    @DisplayName("processPage: existing page -> old version deleted before save")
    void processPage_existingPage_deleted() {
        Site site = site(1L, "https://site.example");
        String pageUrl = "https://site.example/page1";
        Document doc = docWithTitleAndText("Test", "a".repeat(150));
        Page oldPage = new Page("/page1", 200, "old", site);
        oldPage.setId(42L);

        when(indexingState.isActive()).thenReturn(true);
        when(urlFilter.shouldIndex(pageUrl)).thenReturn(true);
        when(crawlerConfig.getRandomDelay()).thenReturn(0);
        when(crawlerConfig.getRetryCount()).thenReturn(1);
        when(seleniumFetcher.shouldUseBrowser(pageUrl)).thenReturn(true);
        when(seleniumFetcher.fetchWithBrowser(pageUrl)).thenReturn(doc);
        when(urlFilter.normalizePath(pageUrl, site.getUrl())).thenReturn("/page1");
        when(pageService.existsByPathAndSite("/page1", site)).thenReturn(true);
        when(pageService.findByPathAndSite("/page1", site)).thenReturn(Optional.of(oldPage));
        when(lemmatizer.getLemmasFrequency(any())).thenReturn(new HashMap<>(Map.of("a", 1)));
        when(pageService.save(any(Page.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pageIndexingService.savePageLemmas(any(Page.class), eq(site), any())).thenReturn(0);

        pageProcessor.processPage(site, pageUrl);

        verify(indexService).deleteByPage(oldPage);
        verify(lemmaService).decrementAllForPage(oldPage);
        verify(pageService).delete(oldPage);
    }

    @Test
    @DisplayName("processPage: title lemmas double weight merged into text lemmas")
    void processPage_titleDoubleWeight() {
        Site site = site(1L, "https://site.example");
        String pageUrl = "https://site.example/page1";
        Document doc = docWithTitleAndText("Test", "a".repeat(150));

        when(indexingState.isActive()).thenReturn(true);
        when(urlFilter.shouldIndex(pageUrl)).thenReturn(true);
        when(crawlerConfig.getRandomDelay()).thenReturn(0);
        when(crawlerConfig.getRetryCount()).thenReturn(1);
        when(seleniumFetcher.shouldUseBrowser(pageUrl)).thenReturn(true);
        when(seleniumFetcher.fetchWithBrowser(pageUrl)).thenReturn(doc);
        when(urlFilter.normalizePath(pageUrl, site.getUrl())).thenReturn("/page1");
        when(pageService.existsByPathAndSite("/page1", site)).thenReturn(false);
        when(lemmatizer.getLemmasFrequency("a".repeat(150))).thenReturn(new HashMap<>(Map.of("a", 1, "b", 2)));
        when(lemmatizer.getLemmasFrequency("Test")).thenReturn(new HashMap<>(Map.of("a", 3, "c", 1)));
        when(pageService.save(any(Page.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pageIndexingService.savePageLemmas(any(Page.class), eq(site), any())).thenReturn(0);

        pageProcessor.processPage(site, pageUrl);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Integer>> captor = ArgumentCaptor.forClass(Map.class);
        verify(pageIndexingService).savePageLemmas(any(Page.class), eq(site), captor.capture());
        Map<String, Integer> merged = captor.getValue();
        assertThat(merged).containsEntry("a", 1 + 3 * 2);
        assertThat(merged).containsEntry("b", 2);
        assertThat(merged).containsEntry("c", 1 * 2);
    }

    @Test
    @DisplayName("processPage: browser null once -> retry -> success")
    void processPage_retry_success() {
        Site site = site(1L, "https://site.example");
        String pageUrl = "https://site.example/page1";
        Document doc = docWithTitleAndText("Test", "a".repeat(150));

        when(indexingState.isActive()).thenReturn(true);
        when(urlFilter.shouldIndex(pageUrl)).thenReturn(true);
        when(crawlerConfig.getRandomDelay()).thenReturn(0);
        when(crawlerConfig.getRetryCount()).thenReturn(3);
        when(crawlerConfig.getRetryDelay()).thenReturn(0);
        when(seleniumFetcher.shouldUseBrowser(pageUrl)).thenReturn(true);
        when(seleniumFetcher.fetchWithBrowser(pageUrl)).thenReturn(null).thenReturn(doc);
        when(urlFilter.normalizePath(pageUrl, site.getUrl())).thenReturn("/page1");
        when(pageService.existsByPathAndSite("/page1", site)).thenReturn(false);
        when(lemmatizer.getLemmasFrequency(any())).thenReturn(new HashMap<>(Map.of("a", 1)));
        when(pageService.save(any(Page.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pageIndexingService.savePageLemmas(any(Page.class), eq(site), any())).thenReturn(0);

        Optional<ProcessedPage> result = pageProcessor.processPage(site, pageUrl);

        assertThat(result).isPresent();
        verify(seleniumFetcher, times(2)).fetchWithBrowser(pageUrl);
    }

    // ===== helpers =====

    private static Site site(Long id, String url) {
        Site s = new Site("Site", url);
        s.setId(id);
        s.setStatus(Status.INDEXING);
        return s;
    }

    private static Document docWithTitleAndText(String title, String text) {
        return Jsoup.parse("<html><head><title>" + title
                + "</title></head><body>" + text + "</body></html>");
    }
}
