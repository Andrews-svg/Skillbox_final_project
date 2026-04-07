//package searchengine.services;
//
//import com.example.searchengine.Application;
//import com.example.searchengine.models.Page;
//import com.example.searchengine.models.Site;
//import com.example.searchengine.repository.PageRepository;
//import com.example.searchengine.repository.SiteRepository;
//import com.example.searchengine.services.SiteService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.transaction.annotation.Transactional;
//
//import java.util.Collections;
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest(classes = {Application.class})
////@DataJpaTest
//class SiteServiceIntegrationTest {
//
//    @Autowired
//    private SiteService siteService;
//
//    @Autowired
//    private SiteRepository siteRepository;
//
//    @Autowired
//    private PageRepository pageRepository;
//
//    @BeforeEach
//    void init() {
//
//    }
//
//    @Test
//    @Transactional
//    void testSaveSite_Successful() throws Exception {
//        Site site = new Site();
//        site.setUrl("https://example.com");
//
//        siteService.saveSite(site);
//
//        Optional<Site> savedSite = siteRepository.findById(site.getId());
//        assertThat(savedSite).isPresent();
//        assertThat(savedSite.get().getUrl()).isEqualTo("https://example.com");
//    }
//
//    @Test
//    @Transactional
//    void testFetchFullyLoadedSite_SuccessfullyFetched() {
//        Long siteId = 1L;
//        Site mockSite = new Site();
//        mockSite.setId(siteId);
//        mockSite.setUrl("https://example.com");
//
//        siteRepository.saveAndFlush(mockSite);
//
//        Page page = new Page();
//        page.setUrl("https://example.com/page");
//        page.setSite(mockSite);
//        pageRepository.saveAndFlush(page);
//
//        List<Page> pages = Collections.singletonList(page);
//
//        Site fetchedSite = siteService.fetchFullyLoadedSite(siteId);
//
//        assertThat(fetchedSite.getId()).isEqualTo(siteId);
//        assertThat(fetchedSite.getPages()).isNotEmpty();
//        assertThat(fetchedSite.getPages().get(0).getUrl()).isEqualTo("https://example.com/page");
//    }
//}