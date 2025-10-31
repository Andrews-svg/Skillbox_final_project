//package searchengine.repository;
//
//import com.example.searchengine.models.Page;
//import com.example.searchengine.config.Site;
//import com.example.searchengine.repository.PageRepository;
//import com.example.searchengine.services.PageService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.Mockito;
//import org.mockito.MockitoAnnotations;
//
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.when;
//
//public class PageRepositoryTest {
//
//    @Mock
//    private PageRepository pageRepository;
//
//    @InjectMocks
//    private PageService pageService;
//
//    @BeforeEach
//    public void setup() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void testSavingPage() throws Exception {
//        Site mockSite = Mockito.mock(Site.class);
//        Page page = new Page(
//                "https://www.test.ru",
//                "/test/path",
//                mockSite
//        );
//        page.setTitle("Test Title");
//        page.setContent("<html><body>Test Content</body></html>");
//
//        when(pageRepository.save(any(Page.class))).thenAnswer(invocation -> {
//            Page savedPage = invocation.getArgument(0);
//            savedPage.setId(1L);
//            return savedPage;
//        });
//
//        Page savedPage = pageService.savePage(page);
//
//        assertNotNull(savedPage.getId(), "ID должен быть установлен после сохранения");
//        assertEquals("Test Title", savedPage.getTitle(), "Заголовок должен совпадать");
//        assertEquals("<html><body>Test Content</body></html>", savedPage.getContent(),
//                "Контент должен совпадать");
//    }
//}
