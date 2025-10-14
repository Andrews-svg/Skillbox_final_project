//package searchengine.models;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
//class PageTest {
//
//    private Page page;
//
//    @BeforeEach
//    void setUp() {
//        page = new Page();
//    }
//
//    @Test
//    void testDefaultConstructor() {
//        assertEquals(0, page.getRelevance());
//        assertNull(page.getUrl());
//        assertNull(page.getPath());
//        assertNull(page.getContent());
//    }
//
//    @Test
//    void testConstructorWithRelevance() {
//        Page pageWithRelevance = new Page(5.0f);
//        assertEquals(5.0f, pageWithRelevance.getRelevance());
//    }
//
//    @Test
//    void testConstructorWithAllFields() {
//        Site mockSite = mock(Site.class);
//        when(mockSite.getId()).thenReturn(1L);
//        Page pageWithAllFields = new Page("http://example.com", "/example", 200L,
//                "Content", mockSite, "Title", "Snippet", 1.0f, Status.INDEXING);
//
//        assertEquals("http://example.com", pageWithAllFields.getUrl());
//        assertEquals("/example", pageWithAllFields.getPath());
//        assertEquals(200L, pageWithAllFields.getCodeOptional().orElse(null));
//        assertEquals("Content", pageWithAllFields.getContent());
//        assertEquals(mockSite, pageWithAllFields.getSite());
//        assertEquals("Title", pageWithAllFields.getTitle());
//        assertEquals("Snippet", pageWithAllFields.getSnippet());
//        assertEquals(1.0f, pageWithAllFields.getRelevance());
//    }
//
//    @Test
//    void testSetUrl() {
//        page.setUrl("http://example.com");
//        assertEquals("http://example.com", page.getUrl());
//    }
//
//    @Test
//    void testSetUrlWithNull() {
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            page.setUrl(null);
//        });
//        assertEquals("URL must not be null or empty", exception.getMessage());
//    }
//
//    @Test
//    void testSetPath() {
//        page.setPath("/example");
//        assertEquals("/example", page.getPath());
//    }
//
//    @Test
//    void testSetPathWithNull() {
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            page.setPath(null);
//        });
//        assertEquals("Path must not be null or empty", exception.getMessage());
//    }
//
//    @Test
//    void testSetCode() {
//        page.setCode(200L);
//        assertEquals(200, page.getCodeOptional().orElse(null));
//    }
//
//    @Test
//    void testSetCodeWithNegativeValue() {
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            page.setCode((long) -1);
//        });
//        assertEquals("Code must be non-negative", exception.getMessage());
//    }
//
//    @Test
//    void testSetRelevance() {
//        page.setRelevance(2.5f);
//        assertEquals(2.5f, page.getRelevance());
//    }
//
//    @Test
//    void testSetRelevanceWithNegativeValue() {
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            page.setRelevance(-1);
//        });
//        assertEquals("Relevance must be non-negative", exception.getMessage());
//    }
//
//    @Test
//    void testEquals() {
//        Page page1 = new Page();
//        page1.setUrl("http://example.com");
//        page1.setId(1L);
//
//        Page page2 = new Page();
//        page2.setUrl("http://example.com");
//        page2.setId(1L);
//
//        Page page3 = new Page();
//        page3.setUrl("http://different.com");
//        page3.setId(2L);
//
//        assertEquals(page1, page2);
//        assertNotEquals(page1, page3);
//        assertNotEquals(null, page1);
//        assertNotEquals(new Object(), page1);
//    }
//
//    @Test
//    void testHashCode() {
//        Page page1 = new Page();
//        page1.setUrl("http://example.com");
//        page1.setId(1L);
//
//        Page page2 = new Page();
//        page2.setUrl("http://example.com");
//        page2.setId(1L);
//
//        assertEquals(page1.hashCode(), page2.hashCode());
//    }
//
//    @Test
//    void testToString() {
//        page.setUrl("http://example.com");
//        page.setPath("/example");
//        String result = page.toString();
//        assertTrue(result.contains("url='" + "http://example.com" + "'"));
//        assertTrue(result.contains("path='" + "/example" + "'"));
//    }
//
//    @Test
//    void testSetContent() {
//        page.setContent("This is a test content.");
//        assertEquals("This is a test content.", page.getContent());
//    }
//
//    @Test
//    void testSetTitle() {
//        page.setTitle("Test Title");
//        assertEquals("Test Title", page.getTitle());
//    }
//
//    @Test
//    void testSetSnippet() {
//        page.setSnippet("This is a snippet.");
//        assertEquals("This is a snippet.", page.getSnippet());
//    }
//
//    @Test
//    void testSetSite() {
//        Site mockSite = mock(Site.class);
//        page.setSite(mockSite);
//        assertEquals(mockSite, page.getSite());
//    }
//
//    @Test
//    void testSetUri() {
//        page.setUri("http://example.com/uri");
//        assertEquals("http://example.com/uri", page.getUri());
//    }
//
//    @Test
//    void testSetUriWithNull() {
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            page.setUri(null);
//        });
//        assertEquals("URI must not be null or empty", exception.getMessage());
//    }
//
//    @Test
//    void testSetUriWithEmpty() {
//        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//            page.setUri("");
//        });
//        assertEquals("URI must not be null or empty", exception.getMessage());
//    }
//}