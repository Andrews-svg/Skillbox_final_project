//package searchengine.repository;
//
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import org.springframework.test.context.TestPropertySource;
//import org.springframework.dao.DataIntegrityViolationException;
//import searchengine.models.Page;
//import searchengine.models.Site;
//import searchengine.models.Status;
//import searchengine.services.PageService;
//
//import java.util.Optional;
//import java.util.UUID;
//
//import static org.jsoup.helper.Validate.fail;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
//@TestPropertySource(locations = "classpath:application-test.properties")
//public class PageRepositoryIntegrationTest {
//
//    @Autowired
//    private PageRepository pageRepository;
//
//    @Autowired
//    private PageService pageService;
//
//    @Autowired
//    private SiteRepository siteRepository;
//
//    private Page testPage;
//    private Site testSite;
//
//    @BeforeEach
//    @Transactional
//    public void setUp() {
//
//
//        testSite = new Site("Test Site", "http://example.com", Status.INDEXING);
//        testSite = siteRepository.save(testSite);
//
//        testPage = new Page("http://example.com", "/example", 200L,
//                "http://example.com/example", "content",
//                testSite, "title", "snippet", 1.0f, Status.INDEXING);
//        pageRepository.save(testPage);
//    }
//
//    @Test
//    public void testCreatePage() {
//        Page page = new Page("http://example.com", "/example", null,
//                "http://example.com/example", "Content",
//                testSite, "Title", "Snippet", 1.0f, Status.INDEXING);
//
//        Assertions.assertNotNull(page);
//        assertEquals("http://example.com", page.getUrl());
//    }
//
//    @Test
//    public void testAddPage() {
//        Site newSite = new Site("New Test Site",
//                "http://newexample.com", Status.INDEXING);
//        siteRepository.save(newSite);
//
//        Page newPage = new Page("http://newexample.com", "/newexample", null,
//                "http://newexample.com/newexample", "New Content",
//                newSite, "New Title", "New Snippet", 1.0f, Status.INDEXING);
//
//        newPage.markAsSuccessful();
//        pageService.addPage(newPage);
//
//        Optional<Page> addedPage = pageRepository.findByPath(newPage.getPath());
//        assertTrue(addedPage.isPresent(), "Страница должна быть добавлена");
//        assertEquals(newPage.getUrl(), addedPage.get().getUrl(),
//                "URL добавленной страницы должен совпадать");
//    }
//
//    @Test
//    public void testExistsByPath() {
//        boolean exists = pageRepository.existsByPath(testPage.getPath());
//        assertTrue(exists, "Страница должна существовать по данному пути");
//    }
//
//    @Test
//    public void testFindByPath() {
//        Optional<Page> actualPage = pageRepository.findByPath(testPage.getPath());
//        assertTrue(actualPage.isPresent(),
//                "Страница должна быть найдена по данному пути");
//        assertEquals(testPage, actualPage.get(),
//                "Возвращаемая страница не совпадает с ожидаемой");
//    }
//
//    @Test
//    public void testCountByUrl() {
//        String url = "http://nonexistent-url.com";
//        long count = pageRepository.countByUrl(url);
//        assertEquals(0, count,
//                "Количество страниц с данным URL должно быть равно 0");
//    }
//
//    @Test
//    public void testCountByExistingUrl() {
//        String url = "http://example.com";
//        long count = pageRepository.countByUrl(url);
//        assertEquals(1, count,
//                "Количество страниц с данным URL должно быть равно 1");
//    }
//
//
//    @Test
//    public void testUpdatePage() {
//        testPage.setTitle("Updated Title");
//        pageRepository.save(testPage);
//
//        Optional<Page> updatedPage = pageRepository.findByPath(testPage.getPath());
//        assertTrue(updatedPage.isPresent(), "Страница должна быть найдена");
//        assertEquals("Updated Title", updatedPage.get().getTitle(),
//                "Заголовок страницы должен быть обновлен");
//    }
//
//    @Test
//    public void testDeletePage() {
//        pageRepository.delete(testPage);
//        Optional<Page> deletedPage = pageRepository.findByPath(testPage.getPath());
//        assertTrue(deletedPage.isEmpty(), "Страница должна быть удалена");
//    }
//
//    @Test
//    public void testSaveDuplicatePage() {
//        // Создаем и сохраняем оригинальную страницу
//        Page originalPage = new Page();
//        originalPage.setUrl("http://example.com/original");
//        originalPage.setUri("/original");
//        originalPage.setPath("/original/path");
//        originalPage.setCode(200L);
//        originalPage.setContent("Original content");
//        originalPage.setTitle("Original Title");
//        originalPage.setSnippet("Original Snippet");
//        originalPage.setRelevance(1.0f);
//        originalPage.setSite(testSite);
//
//        pageRepository.save(originalPage);
//
//        Page duplicatePage = new Page();
//        duplicatePage.setUrl(originalPage.getUrl());
//        duplicatePage.setUri(originalPage.getUri());
//        duplicatePage.setPath("/duplicate/path");
//        duplicatePage.setCode(200L);
//        duplicatePage.setContent("Duplicate content");
//        duplicatePage.setTitle("Duplicate Title");
//        duplicatePage.setSnippet("Duplicate Snippet");
//        duplicatePage.setRelevance(1.0f);
//        duplicatePage.setSite(testSite);
//
//        Exception exception = assertThrows(DataIntegrityViolationException.class, () -> {
//            pageRepository.save(duplicatePage);
//        });
//
//        String expectedMessage = "could not execute statement";
//        assertTrue(exception.getMessage().contains(expectedMessage),
//                "Сообщение об ошибке должно содержать информацию о дубликате");
//    }
//
//    @Test
//    public void testFindNonExistentPage() {
//        Optional<Page> nonExistentPage = pageRepository.findByPath("/nonexistent");
//        assertTrue(nonExistentPage.isEmpty(), "Страница не должна существовать");
//    }
//
//    @Test
//    public void testSavePageWithoutCodeThrowsException() {
//        Page pageWithoutCode = new Page();
//        pageWithoutCode.setPath("/example");
//        pageWithoutCode.setSite(testSite);
//        pageWithoutCode.setContent("Content without code");
//        pageWithoutCode.setTitle("Title without code");
//        pageWithoutCode.setSnippet("Snippet without code");
//        pageWithoutCode.setRelevance(1.0f);
//
//        Exception exception = assertThrows(DataIntegrityViolationException.class, () -> {
//            pageRepository.save(pageWithoutCode);
//        });
//
//        String expectedMessage = "not-null property references a null or transient value";
//        assertTrue(exception.getMessage().contains(expectedMessage),
//                "Сообщение об ошибке должно указывать на отсутствие кода");
//    }
//
//    @Test
//    public void testFindPageById() {
//        Optional<Page> foundPage = pageRepository.findById(testPage.getId());
//        assertTrue(foundPage.isPresent(), "Страница должна быть найдена по ID");
//        assertEquals(testPage, foundPage.get(),
//                "Возвращаемая страница не совпадает с ожидаемой");
//    }
//
//    @Test
//    public void testCreatePageWithDefaultConstructor() {
//        Page page = new Page();
//
//        String uniqueUrl = "http://example.com/" + UUID.randomUUID().toString();
//        page.setUrl(uniqueUrl);
//        page.setPath("/example");
//        page.setContent("content");
//        page.setSite(testSite);
//        page.setTitle("title");
//        page.setSnippet("snippet");
//        page.setRelevance(1.0f);
//        page.setStatus(Status.INDEXING);
//
//        page.setUri("http://example.com/page/" + UUID.randomUUID().toString());
//
//        pageRepository.save(page);
//
//        assertEquals(200L, page.getCode(),
//                "Поле code должно инициализироваться в 200L");
//    }
//
//    @Test
//    public void testCreatePageWithParameterizedConstructor() {
//        Page page = new Page("http://unique-example.com",
//                "/unique-path", null,
//                "http://unique-example.com/example", "content",
//                testSite, "title", "snippet", 1.0f, Status.INDEXING);
//
//        try {
//            pageRepository.save(page);
//        } catch (Exception e) {
//            e.printStackTrace();
//            fail("Ошибка при сохранении страницы: " + e.getMessage());
//        }
//
//        assertEquals(200L, page.getCode(),
//                "Поле code должно инициализироваться в 200L");
//    }
//
//
//    @Test
//    public void testCreatePageAndSetCodeExplicitly() {
//        Page page = new Page();
//        page.setCode(200L);
//
//        String uniqueId = UUID.randomUUID().toString();
//        page.setUrl("http://example.com/" + uniqueId);
//        page.setPath("/example");
//        page.setContent("content");
//        page.setSite(testSite);
//        page.setTitle("title");
//        page.setSnippet("snippet");
//        page.setRelevance(1.0f);
//        page.setStatus(Status.INDEXING);
//
//        page.setUri("http://example.com/page/" + uniqueId);
//
//        pageRepository.save(page);
//
//        assertEquals(200L, page.getCode(),
//                "Поле code должно быть установлено в 200L");
//    }
//}