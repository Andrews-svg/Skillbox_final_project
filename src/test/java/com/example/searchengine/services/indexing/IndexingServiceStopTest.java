package com.example.searchengine.services.indexing;

import com.example.searchengine.config.CrawlerConfig;
import com.example.searchengine.config.SitesList;
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

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexingService - stop is final (P1c)")
class IndexingServiceStopTest {

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

    @Test
    @DisplayName("P1c: после stopIndexing флаг финален, page после стопа работает как новая операция")
    void stopIndexing_makesStopFinal_pageAfterStopWorksAsNewOperation() {
        indexingState.startIndexing();
        assertThat(indexingState.isActive()).isTrue();

        indexingService.stopIndexing();
        assertThat(indexingState.isActive()).isFalse();

        indexingState.startIndexing();
        assertThat(indexingState.isActive())
                .as("после stopIndexing reset() должен снять stopRequested — page работает как новая")
                .isTrue();
        indexingState.endIndexing();
        assertThat(indexingState.isActive()).isFalse();
    }

    @Test
    @DisplayName("P1c: stopIndexing обнуляет activeCount — никакой залипший full-воркер не удержит флаг")
    void stopIndexing_resetsActiveCount_orphanFullWorkerCannotKeepFlag() {
        indexingState.startIndexing();
        indexingState.startIndexing();
        assertThat(indexingState.isActive()).isTrue();

        indexingService.stopIndexing();
        assertThat(indexingState.isActive()).isFalse();
        indexingState.startIndexing();
        assertThat(indexingState.isActive()).isTrue();
    }
}
