//package searchengine.services;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import searchengine.config.SitesList;
//import searchengine.dao.SiteDao;
//import searchengine.models.Site;
//import searchengine.models.Status;
//import searchengine.repository.SiteRepository;
//
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class SiteServiceTest {
//
//    @Mock
//    private SiteDao siteDao;
//
//    @Mock
//    private SitesList sitesList;
//
//    @Mock
//    private SiteRepository siteRepository;
//
//    @InjectMocks
//    private SiteService siteService;
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void testSaveSite() {
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//        when(siteRepository.save(site)).thenReturn(site);
//
//        siteService.save(site);
//
//        verify(siteRepository, times(1)).save(site);
//    }
//
//    @Test
//    public void testFindByUrl() {
//        String url = "http://testsite.com";
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//        when(siteRepository.findByUrl(url)).thenReturn(Optional.of(site));
//        Optional<Site> foundSite = siteService.findByUrl(url);
//
//        assertTrue(foundSite.isPresent(), "Site should be found");
//        assertEquals(site, foundSite.get(), "The found site should match the expected site");
//    }
//
//    @Test
//    public void testGetTotalSites() {
//        when(siteRepository.count()).thenReturn(10L);
//
//        long totalSites = siteService.getTotalSites();
//
//        assertEquals(10L, totalSites, "Total sites count should match");
//    }
//
//    @Test
//    public void testFindById() {
//        Long siteId = 1L;
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//        when(siteRepository.findById(siteId)).thenReturn(Optional.of(site));
//
//        Optional<Site> foundSite = siteService.findById(siteId);
//
//        assertTrue(foundSite.isPresent(), "Site should be found");
//        assertEquals(site, foundSite.get(), "The found site should match the expected site");
//    }
//
//    @Test
//    public void testUpdateSite() {
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//
//        siteService.updateSite(site);
//
//        verify(siteDao, times(1)).update(site);
//    }
//
//    @Test
//    public void testExistsByUrl() {
//        String url = "http://testsite.com";
//        when(siteRepository.existsByUrl(url)).thenReturn(true);
//
//        boolean exists = siteService.existsByUrl(url);
//
//        assertTrue(exists, "Site should exist for the given URL");
//        verify(siteRepository, times(1)).existsByUrl(url);
//    }
//
//    @Test
//    public void testFindAllSites() {
//        SitesList.SiteConfig config = new SitesList.SiteConfig();
//        config.setName("Test Site");
//        config.setUrl("http://testsite.com");
//
//        Map<Long, SitesList.SiteConfig> sitesMap = new HashMap<>();
//        sitesMap.put(1L, config);
//
//        when(sitesList.getSites()).thenReturn(sitesMap);
//
//        List<Site> sites = siteService.findAllSites();
//
//        assertEquals(1, sites.size(), "There should be one site");
//        assertEquals("Test Site", sites.get(0).getName(), "The site name should match");
//    }
//
//    @Test
//    public void testDeleteSite() {
//        Site site = new Site("Test Site", "http://example.com", Status.INDEXING);
//
//        siteService.delete(site);
//
//        verify(siteRepository, times(1)).delete(site);
//    }
//
//    @Test
//    public void testMarkAllSitesAsFailed() {
//        siteService.markAllSitesAsFailed();
//
//        verify(siteDao, times(1)).markAllSitesAsFailed();
//    }
//
//    @Test
//    public void testCheckIfSiteExists() {
//        String siteName = "Test Site";
//        when(siteDao.checkIfSiteExists(siteName)).thenReturn(true);
//
//        boolean exists = siteService.checkIfSiteExists(siteName);
//
//        assertTrue(exists, "Site should exist");
//        verify(siteDao, times(1)).checkIfSiteExists(siteName);
//    }
//}