package com.example.searchengine.services.indexing;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.SessionStatus;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.LemmaService;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.services.crawler.CrawlerService;
import com.example.searchengine.services.indexing.IndexService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexingService - indexPage / stopIndexing / onPageIndexed contract")
class IndexingServiceTest {

    @Mock private SiteService siteService;
    @Mock private PageService pageService;
    @Mock private LemmaService lemmaService;
    @Mock private IndexService indexService;
    @Mock private CrawlerService crawlerService;
    @Mock private SessionHistoryService sessionHistoryService;

    private SitesList sitesList;
    private CrawlerConfig crawlerConfig;
    private IndexingState indexingState;
    private IndexingService indexingService;

    @BeforeEach
    void setUp() {
        sitesList = new SitesList();
        crawlerConfig = new CrawlerConfig();
        indexingState = new IndexingState();
        indexingService = new IndexingService(
                siteService, pageService, lemmaService, indexService,
                crawlerService, sitesList, indexingState, crawlerConfig,
                sessionHistoryService);
    }

    @AfterEach
    void tearDown() {
        indexingState.reset();
        indexingService.destroy();
    }

    // ===== startFullIndexing: 2 =====

    @Test
    @DisplayName("startFullIndexing: already active -> IllegalStateException, no side effects")
    void startFullIndexing_alreadyActive_throws() {
        indexingState.startIndexing();

        assertThatThrownBy(() -> indexingService.startFullIndexing())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Индексация уже запущена");

        assertThat(indexingState.isActive()).isTrue();
    }

    @Test
    @DisplayName("startFullIndexing: empty sites config -> IllegalStateException, active reset")
    void startFullIndexing_noSitesInConfig_throwsAndResetsActive() {
        sitesList.setSites(Collections.emptyList());

        assertThatThrownBy(() -> indexingService.startFullIndexing())
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("В конфигурации нет сайтов для индексации");

        assertThat(indexingState.isActive()).isFalse();
    }

    // ===== indexPage: 6 =====

    @Test
    @DisplayName("indexPage: url not from any configured site -> false, active reset")
    void indexPage_urlNotFromConfig_returnsFalseAndResetsActive() {
        sitesList.setSites(List.of(new SitesList.SiteConfig("Site", "https://site.example")));

        boolean result = indexingService.indexPage("https://other.example/page");

        assertThat(result).isFalse();
        assertThat(indexingState.isActive()).isFalse();
        verifyNoInteractions(crawlerService);
    }

    @Test
    @DisplayName("indexPage: url from config, crawler true, no existing page -> true, active reset")
    void indexPage_fromConfig_crawlerTrue_returnsTrue() {
        sitesList.setSites(List.of(new SitesList.SiteConfig("Site", "https://site.example")));
        Site site = site(1L, "https://site.example", Status.INDEXED);
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(pageService.findByPathAndSite(eq("/page"), eq(site))).thenReturn(Optional.empty());
        when(crawlerService.indexPage(site, "https://site.example/page")).thenReturn(true);

        boolean result = indexingService.indexPage("https://site.example/page");

        assertThat(result).isTrue();
        assertThat(indexingState.isActive()).isFalse();
        verify(pageService, never()).delete(any());
        verify(indexService, never()).deleteByPage(any());
        verify(lemmaService, never()).decrementAllForPage(any());
    }

    @Test
    @DisplayName("indexPage: crawler false -> false, active reset")
    void indexPage_crawlerFalse_returnsFalse() {
        sitesList.setSites(List.of(new SitesList.SiteConfig("Site", "https://site.example")));
        Site site = site(1L, "https://site.example", Status.INDEXED);
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(pageService.findByPathAndSite(eq("/page"), eq(site))).thenReturn(Optional.empty());
        when(crawlerService.indexPage(site, "https://site.example/page")).thenReturn(false);

        boolean result = indexingService.indexPage("https://site.example/page");

        assertThat(result).isFalse();
        assertThat(indexingState.isActive()).isFalse();
    }

    @Test
    @DisplayName("indexPage: чужой ref-count не задет — параллельная операция продолжает держать active=true")
    void indexPage_foreignRefCountNotAffected_staysActive() {
        indexingState.startIndexing();
        sitesList.setSites(List.of(new SitesList.SiteConfig("Site", "https://site.example")));
        Site site = site(1L, "https://site.example", Status.INDEXED);
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(pageService.findByPathAndSite(eq("/page"), eq(site))).thenReturn(Optional.empty());
        when(crawlerService.indexPage(site, "https://site.example/page")).thenReturn(true);

        boolean result = indexingService.indexPage("https://site.example/page");

        assertThat(result).isTrue();
        assertThat(indexingState.isActive()).isTrue();
    }

    @Test
    @DisplayName("indexPage: crawler throws -> false, active reset")
    void indexPage_crawlerThrows_returnsFalseAndResetsActive() {
        sitesList.setSites(List.of(new SitesList.SiteConfig("Site", "https://site.example")));
        Site site = site(1L, "https://site.example", Status.INDEXED);
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(pageService.findByPathAndSite(eq("/page"), eq(site))).thenReturn(Optional.empty());
        when(crawlerService.indexPage(site, "https://site.example/page"))
                .thenThrow(new RuntimeException("boom"));

        boolean result = indexingService.indexPage("https://site.example/page");

        assertThat(result).isFalse();
        assertThat(indexingState.isActive()).isFalse();
    }

    @Test
    @DisplayName("indexPage: existing page removed (index + lemmas + page)")
    void indexPage_existingPage_removedBeforeCrawl() {
        sitesList.setSites(List.of(new SitesList.SiteConfig("Site", "https://site.example")));
        Site site = site(1L, "https://site.example", Status.INDEXED);
        Page existing = new Page("/page", 200, "<html><body>old</body></html>", site);
        existing.setId(99L);
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(pageService.findByPathAndSite(eq("/page"), eq(site))).thenReturn(Optional.of(existing));
        when(crawlerService.indexPage(site, "https://site.example/page")).thenReturn(true);

        boolean result = indexingService.indexPage("https://site.example/page");

        assertThat(result).isTrue();
        verify(indexService).deleteByPage(existing);
        verify(lemmaService).decrementAllForPage(existing);
        verify(pageService).delete(existing);
    }

    // ===== stopIndexing: 3 =====

    @Test
    @DisplayName("stopIndexing: not active -> early return, crawler untouched")
    void stopIndexing_notActive_earlyReturn() {
        indexingService.stopIndexing();

        verifyNoInteractions(crawlerService);
        verifyNoInteractions(sessionHistoryService);
        assertThat(indexingState.isActive()).isFalse();
    }

    @Test
    @DisplayName("stopIndexing: active -> requestStop + reset, crawler stopped, session saved")
    void stopIndexing_active_stopsAndSavesSession() {
        indexingState.startIndexing();
        indexingState.startSession();
        Site site = site(7L, "https://site.example", Status.INDEXING);
        indexingState.addSessionSite(7L);
        when(siteService.findById(7L)).thenReturn(Optional.of(site));

        indexingService.stopIndexing();

        assertThat(indexingState.isActive()).isFalse();
        verify(crawlerService).stopAllCrawling();
        verify(siteService).updateStatusWithError(eq(site), anyString());
        verify(sessionHistoryService).saveSession(
                any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                eq(SessionStatus.STOPPED), any());
    }

    @Test
    @DisplayName("stopIndexing: active, crawler throws -> still deactivates and saves session")
    void stopIndexing_crawlerThrows_continuesGracefully() {
        indexingState.startIndexing();
        indexingState.startSession();
        org.mockito.Mockito.doThrow(new RuntimeException("crawler down"))
                .when(crawlerService).stopAllCrawling();

        indexingService.stopIndexing();

        assertThat(indexingState.isActive()).isFalse();
        verify(sessionHistoryService).saveSession(
                any(), any(), any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                eq(SessionStatus.STOPPED), any());
    }

    // ===== onPageIndexed: 2 =====

    @Test
    @DisplayName("onPageIndexed: not active -> no status update")
    void onPageIndexed_notActive_noop() {
        Site site = site(1L, "https://site.example", Status.INDEXING);

        indexingService.onPageIndexed(site);

        verify(siteService, never()).updateStatusTime(any());
    }

    @Test
    @DisplayName("onPageIndexed: active -> status time updated")
    void onPageIndexed_active_updatesStatusTime() {
        indexingState.startIndexing();
        Site site = site(1L, "https://site.example", Status.INDEXING);

        indexingService.onPageIndexed(site);

        verify(siteService).updateStatusTime(site);
    }

    // ===== helpers =====

    private static Site site(Long id, String url, Status status) {
        Site s = new Site("Site", url);
        s.setId(id);
        s.setStatus(status);
        return s;
    }
}