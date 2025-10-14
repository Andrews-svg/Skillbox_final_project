//package searchengine.repository;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.mockito.Mockito.when;
//
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import searchengine.config.SitesList;
//import searchengine.models.Site;
//import searchengine.models.Status;
//import searchengine.services.SiteService;
//
//import java.util.*;
//
//
//public class SiteRepositoryTest {
//
//    @Mock
//    private SiteRepository siteRepository;
//
//    @Mock
//    private SitesList sitesList;
//
//
//    @InjectMocks
//    private SiteService siteService;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//
//    }
//
//    @Test
//    public void testFindAllSitesReturnsOnlyMatchingSites() {
//        SitesList.SiteConfig siteConfig1 =
//                new SitesList.SiteConfig("Lenta", "https://www.lenta.ru/article");
//        SitesList.SiteConfig siteConfig2 =
//                new SitesList.SiteConfig("Skillbox", "https://www.skillbox.ru/course");
//        SitesList.SiteConfig siteConfig3 =
//                new SitesList.SiteConfig("Playback", "https://www.playback.ru/video");
//
//        Map<Long, SitesList.SiteConfig> sitesMap = new HashMap<>();
//        sitesMap.put(1L, siteConfig1);
//        sitesMap.put(2L, siteConfig2);
//        sitesMap.put(3L, siteConfig3);
//
//        when(sitesList.getSites()).thenReturn(sitesMap);
//
//        List<Site> foundSites = siteService.findAllSites();
//
//        assertThat(foundSites).isNotEmpty();
//        assertThat(foundSites).hasSize(3);
//        assertThat(foundSites).extracting(Site::getUrl)
//                .containsExactlyInAnyOrder(
//                        "https://www.lenta.ru/article",
//                        "https://www.skillbox.ru/course",
//                        "https://www.playback.ru/video"
//                );
//    }
//
//
//    @Test
//    public void testFindByUrl() {
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//        when(siteRepository.findByUrl("https://example.com")).thenReturn(Optional.of(site));
//        Optional<Site> foundSite = siteService.findByUrl("https://example.com");
//
//        assertThat(foundSite).isPresent();
//        assertThat(foundSite.get().getUrl()).isEqualTo("http://example.com");
//    }
//
//    @Test
//    public void testFindByUrlReturnsSingleSite() {
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//        when(siteRepository.findByUrl("https://example.com/page1")).thenReturn(Optional.of(site));
//        Optional<Site> optionalSite = siteService.findByUrl("https://example.com/page1");
//
//        assertThat(optionalSite).isPresent();
//        assertThat(optionalSite.get().getUrl()).isEqualTo("http://example.com");
//    }
//
//
//    @Test
//    public void testExistsByUrl() {
//        when(siteRepository.existsByUrl("https://example.com")).thenReturn(true);
//
//        boolean exists = siteService.existsByUrl("https://example.com");
//        assertThat(exists).isTrue();
//    }
//
//
//    @Test
//    public void testFindByUrlNotFound() {
//        when(siteRepository.findByUrl("https://nonexistent.com")).thenReturn(Optional.empty());
//
//        Optional<Site> foundSite = siteService.findByUrl("https://nonexistent.com");
//        assertThat(foundSite).isNotPresent();
//    }
//}