//package searchengine.services;
//
//import com.example.searchengine.config.Site;
//import com.example.searchengine.dto.statistics.DetailedStatisticsItem;
//import com.example.searchengine.dao.PageDao;
//import com.example.searchengine.dao.LemmaDao;
//import com.example.searchengine.models.Status;
//import com.example.searchengine.services.PageService;
//import com.example.searchengine.services.SiteService;
//import com.example.searchengine.services.StatisticsServiceImpl;
//import com.example.searchengine.services.LemmaService;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.Map;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//class StatisticsServiceTest {
//
//    @Mock
//    private SiteService siteService;
//
//    @Mock
//    private PageService pageService;
//
//    @Mock
//    private LemmaService lemmaService;
//
//    @Mock
//    private PageDao pageDao;
//
//    @Mock
//    private LemmaDao lemmaDao;
//
//    private StatisticsServiceImpl statisticsService;
//
//    @BeforeEach
//    void initMocks() {
//        MockitoAnnotations.openMocks(this);
//        statisticsService = new StatisticsServiceImpl(siteService, pageService, lemmaDao,
//                lemmaService, null, pageDao);
//    }
//
//    @Test
//    void testGetDetailed() {
//        Site site = new Site();
//        site.setId(1);
//        site.setUrl("https://test-site.ru");
//        site.setName("Тестовый сайт");
//        site.setStatus(Status.INDEXED);
//
//        when(siteService.fetchFullyLoadedSite(1L)).thenReturn(site);
//        when(pageDao.countPagesGroupedBySite(any())).thenReturn(Collections.singletonMap(1L, 10L));
//        when(lemmaDao.countLemmasGroupedBySite(any())).thenReturn(Collections.singletonMap(1L, 5L));
//
//        Map<Long, Long> pagesCountMap = new HashMap<>();
//        pagesCountMap.put(1L, 10L);
//
//        Map<Long, Long> lemmasCountMap = new HashMap<>();
//        lemmasCountMap.put(1, 5);
//
//        DetailedStatisticsItem result =
//                statisticsService.getDetailed(site, pagesCountMap, lemmasCountMap);
//
//        assertNotNull(result);
//        assertEquals("https://test-site.ru", result.getUrl());
//        assertEquals("Тестовый сайт", result.getName());
//        assertEquals(Status.INDEXED, result.getStatus());
//        assertEquals(10L, result.getPages());
//        assertEquals(5L, result.getLemmas());
//    }
//}