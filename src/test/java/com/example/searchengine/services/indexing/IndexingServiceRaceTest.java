package com.example.searchengine.services.indexing;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.config.SitesList;
import com.example.searchengine.models.Site;
import com.example.searchengine.services.LemmaService;
import com.example.searchengine.services.PageService;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.services.crawler.CrawlerService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexingService — wasActive race (P1b)")
class IndexingServiceRaceTest {

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
        indexingState.setActive(false);
        indexingService.destroy();
    }

    @Test
    @DisplayName("P1b: page-index, стартовавшая при active=true, не скипается после завершения full")
    void pageIndexStartedDuringFull_isNotSkippedAfterFullEnds() throws Exception {
        indexingState.setActive(true);
        setUpSitesListWithOneSite();

        CountDownLatch pageEnteredCrawler = new CountDownLatch(1);
        CountDownLatch fullEnded = new CountDownLatch(1);

        when(crawlerService.indexPage(any(Site.class), eq("http://site/page")))
                .thenAnswer(inv -> {
                    pageEnteredCrawler.countDown();
                    if (!fullEnded.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("fullEnded latch не открыт");
                    }
                    return indexingState.isActive();
                });

        ExecutorService ex = Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> result = ex.submit(
                    () -> indexingService.indexPage("http://site/page"));

            assertThat(pageEnteredCrawler.await(5, TimeUnit.SECONDS))
                    .as("indexPage должен дойти до crawlerService.indexPage")
                    .isTrue();

            indexingState.setActive(false);
            fullEnded.countDown();

            assertThat(result.get(5, TimeUnit.SECONDS))
                    .as("indexPage не должна вернуть false только потому, что full завершилась в полёте")
                    .isTrue();
        } finally {
            ex.shutdownNow();
        }
    }

    private void setUpSitesListWithOneSite() {
        SitesList.SiteConfig config = new SitesList.SiteConfig();
        config.setUrl("http://site");
        config.setName("site");
        sitesList.setSites(List.of(config));

        Site site = new Site();
        site.setUrl("http://site");
        site.setName("site");

        when(siteService.findByUrl("http://site")).thenReturn(Optional.of(site));
        when(pageService.findByPathAndSite(anyString(), any(Site.class)))
                .thenReturn(Optional.empty());
    }
}