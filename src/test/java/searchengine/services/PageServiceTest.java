//package searchengine.services;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.MockitoAnnotations;
//import searchengine.dao.PageDao;
//import searchengine.models.Page;
//import searchengine.models.Site;
//import searchengine.models.Status;
//import searchengine.repository.PageRepository;
//import java.util.List;
//import java.util.Optional;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//public class PageServiceTest {
//
//    @Mock
//    private PageRepository pageRepository;
//
//    @Mock
//    private PageDao pageDao;
//
//    @InjectMocks
//    private PageService pageService;
//
//
//
//    @BeforeEach
//    public void setUp() {
//        MockitoAnnotations.openMocks(this);
//    }
//
//    @Test
//    public void testExistsByPath() {
//        String path = "/test/path";
//        when(pageRepository.existsByPath(path)).thenReturn(true);
//
//        Boolean exists = pageService.checkIfPageExists(path);
//        assertTrue(exists, "Страница должна существовать по данному пути");
//    }
//
//    @Test
//    public void testFindByPath() {
//        String path = "/test/path";
//        Page expectedPage = new Page();
//        expectedPage.setPath(path);
//
//        when(pageRepository.findByPath(path)).thenReturn(Optional.of(expectedPage));
//
//        Optional<Page> actualPage = pageService.findByPath(path);
//        assertTrue(actualPage.isPresent(), "Страница должна быть найдена по данному пути");
//        assertEquals(expectedPage, actualPage.get(), "Возвращаемая страница не совпадает с ожидаемой");
//    }
//
//    @Test
//    public void testCountByUrl() {
//        String url = "http://example.com";
//        when(pageRepository.countByUrl(url)).thenReturn(5L);
//
//        long count = pageService.getCountBySiteUrl(url);
//        assertEquals(5, count, "Количество страниц с данным URL должно быть равно 5");
//    }
//
//    @Test
//    public void testCountPagesOnSite() {
//        long siteID = 1L;
//        when(pageDao.countPagesOnSite(siteID)).thenReturn(10L);
//
//        Long count = pageService.countPagesOnSite(siteID);
//        assertEquals(10L, count, "Количество страниц на сайте должно быть равно 10");
//    }
//
//    @Test
//    public void testCheckIfPageExists() {
//        String path = "/test/path";
//        when(pageRepository.existsByPath(path)).thenReturn(true);
//
//        Boolean exists = pageService.checkIfPageExists(path);
//        assertTrue(exists, "Страница должна существовать по данному пути");
//    }
//
//    @Test
//    public void testSavePage() {
//        Page page = new Page();
//        page.setPath("/test/path");
//        page.setUrl("http://example.com/test/path");
//        page.setContent("Sample content");
//        page.setSite(new Site());
//        page.setStatus(Status.INDEXING);
//
//        Page savedPage = new Page();
//        savedPage.setId(1L);
//        savedPage.setPath(page.getPath());
//        savedPage.setUrl(page.getUrl());
//        savedPage.setContent(page.getContent());
//        savedPage.setSite(page.getSite());
//        savedPage.setStatus(page.getStatus());
//
//        when(pageRepository.save(page)).thenReturn(savedPage);
//
//        long pageId = pageService.validateAndSavePage(page);
//        assertEquals(savedPage.getId(), pageId, "ID сохраненной страницы должен совпадать с ожидаемым");
//    }
//
//
//    @Test
//    public void testGetCountBySiteUrl() {
//        String url = "http://example.com";
//        when(pageRepository.countByUrl(url)).thenReturn(5L);
//
//        long count = pageService.getCountBySiteUrl(url);
//        assertEquals(5, count, "Количество страниц с данным URL должно быть равно 5");
//    }
//
//    @Test
//    public void testDeletePage() {
//        Page page = new Page();
//        page.setId(1L);
//
//        doNothing().when(pageRepository).delete(page);
//        pageService.deletePage(page);
//
//        verify(pageRepository, times(1)).delete(page);
//    }
//
//    @Test
//    public void testGetTotalPages() {
//        when(pageRepository.count()).thenReturn(10L);
//
//        int total = pageService.getTotalPages();
//        assertEquals(10, total, "Общее количество страниц должно быть равно 10");
//    }
//
//    @Test
//    public void testFindPage() {
//        long id = 1L;
//        Page expectedPage = new Page();
//        expectedPage.setId(id);
//
//        when(pageRepository.findById(id)).thenReturn(Optional.of(expectedPage));
//
//        Optional<Page> actualPage = pageService.findPage(id);
//        assertTrue(actualPage.isPresent(), "Страница должна быть найдена по данному ID");
//        assertEquals(expectedPage, actualPage.get(), "Возвращаемая страница не совпадает с ожидаемой");
//    }
//
//    @Test
//    public void testFindAllPages() {
//        Page page1 = new Page();
//        Page page2 = new Page();
//        List<Page> expectedPages = List.of(page1, page2);
//
//        when(pageRepository.findAll()).thenReturn(expectedPages);
//
//        List<Page> actualPages = pageService.findAllPages();
//        assertEquals(expectedPages, actualPages, "Список страниц не совпадает с ожидаемым");
//    }
//
//    @Test
//    public void testCountPages() {
//        when(pageRepository.count()).thenReturn(15L);
//
//        long count = pageService.countPages();
//        assertEquals(15L, count, "Количество страниц должно быть равно 15");
//    }
//
//    @Test
//    public void testFindAllBySiteId() {
//        long siteID = 1L;
//        Page page1 = new Page();
//        Page page2 = new Page();
//        List<Page> expectedPages = List.of(page1, page2);
//
//        when(pageDao.findAllbySiteId(siteID)).thenReturn(expectedPages);
//
//        List<Page> actualPages = pageService.findAllBySiteId(siteID);
//        assertEquals(expectedPages, actualPages, "Список страниц для данного siteID не совпадает с ожидаемым");
//    }
//
//    @Test
//    public void testDeleteAllPages() {
//        doNothing().when(pageRepository).deleteAll();
//        pageService.deleteAllPages();
//
//        verify(pageRepository, times(1)).deleteAll();
//    }
//}