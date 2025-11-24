package searchengine.services;

import com.example.searchengine.Application;
import com.example.searchengine.config.Site;
import com.example.searchengine.services.SiteService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = Application.class)
public class SiteServiceIntegrationTest {

    @Autowired
    private SiteService siteService;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testSaveSite() throws Exception {
        Site site = new Site();
        site.setUrl("https://example.com");
        site.setName("Example Website");

        siteService.saveSite(site);
        assertNotNull(site.getId());
    }
}