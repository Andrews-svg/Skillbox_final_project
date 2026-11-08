package com.example.searchengine.services;

import com.example.searchengine.dto.search.SearchData;
import com.example.searchengine.dto.search.SearchResponse;
import com.example.searchengine.models.Index;
import com.example.searchengine.models.Lemma;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.indexing.IndexService;
import com.example.searchengine.utils.Lemmatizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchService - search() contract")
class SearchServiceTest {

    @Mock private LemmaService lemmaService;
    @Mock private IndexService indexService;
    @Mock private SiteService siteService;
    @Mock private Lemmatizer lemmatizer;

    private SearchService searchService;

    @BeforeEach
    void setUp() {
        searchService = new SearchService(lemmaService, indexService, siteService, lemmatizer);
    }

    // ===== 1-2: input validation =====

    @Test
    @DisplayName("query == null -> IllegalArgumentException, dependencies untouched")
    void search_nullQuery_throwsIllegalArgument() {
        assertThatThrownBy(() -> searchService.search(null, null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Задан пустой поисковый запрос");

        verifyNoInteractions(lemmatizer, lemmaService, indexService, siteService);
    }

    @Test
    @DisplayName("query is blank -> IllegalArgumentException")
    void search_blankQuery_throwsIllegalArgument() {
        assertThatThrownBy(() -> searchService.search("   ", null, 0, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Задан пустой поисковый запрос");

        verifyNoInteractions(lemmatizer, lemmaService, indexService, siteService);
    }

    // ===== 3: no lemmas extracted =====

    @Test
    @DisplayName("lemmatizer returns no lemmas -> empty success, no site lookup")
    void search_noLemmas_returnsEmptySuccess() {
        when(lemmatizer.getUniqueLemmas("abc")).thenReturn(Collections.emptySet());

        SearchResponse response = searchService.search("abc", null, 0, 10);

        assertThat(response.isResult()).isTrue();
        assertThat(response.getCount()).isZero();
        assertThat(response.getData()).isEmpty();

        verifyNoInteractions(siteService);
    }

    // ===== 4-5: siteUrl given =====

    @Test
    @DisplayName("siteUrl given, site not found -> empty success")
    void search_siteUrlGiven_siteNotFound_returnsEmpty() {
        when(lemmatizer.getUniqueLemmas("word")).thenReturn(Set.of("word"));
        when(siteService.findByUrl("https://missing.example")).thenReturn(Optional.empty());

        SearchResponse response = searchService.search("word", "https://missing.example", 0, 10);

        assertThat(response.isResult()).isTrue();
        assertThat(response.getCount()).isZero();
        assertThat(response.getData()).isEmpty();
        verifyNoInteractions(lemmaService, indexService);
    }

    @Test
    @DisplayName("siteUrl given, site status INDEXING -> empty success")
    void search_siteUrlGiven_siteNotIndexed_returnsEmpty() {
        Site site = site("https://site.example", "Site", Status.INDEXING);
        when(lemmatizer.getUniqueLemmas("word")).thenReturn(Set.of("word"));
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));

        SearchResponse response = searchService.search("word", site.getUrl(), 0, 10);

        assertThat(response.isResult()).isTrue();
        assertThat(response.getCount()).isZero();
        assertThat(response.getData()).isEmpty();
        verifyNoInteractions(lemmaService, indexService);
    }

    // ===== 6-7: siteUrl null =====

    @Test
    @DisplayName("siteUrl null: only INDEXED sites searched, FAILED skipped")
    void search_noSiteUrl_onlyIndexedSitesUsed() {
        Site indexed = site("https://idx.example", "Idx", Status.INDEXED);
        Site failed  = site("https://fail.example", "Fail", Status.FAILED);

        when(lemmatizer.getUniqueLemmas("word")).thenReturn(Set.of("word"));
        when(siteService.findAll()).thenReturn(List.of(indexed, failed));
        when(lemmaService.findAllByLemmaInAndSite(anySet(), eq(indexed)))
                .thenReturn(Collections.emptyList());

        SearchResponse response = searchService.search("word", null, 0, 10);

        assertThat(response.getCount()).isZero();
        assertThat(response.getData()).isEmpty();

        ArgumentCaptor<Site> captor = ArgumentCaptor.forClass(Site.class);
        verify(lemmaService).findAllByLemmaInAndSite(anySet(), captor.capture());
        assertThat(captor.getValue()).isSameAs(indexed);
    }

    @Test
    @DisplayName("siteUrl null, no sites in DB -> empty success")
    void search_noSiteUrl_noSites_returnsEmpty() {
        when(lemmatizer.getUniqueLemmas("word")).thenReturn(Set.of("word"));
        when(siteService.findAll()).thenReturn(Collections.emptyList());

        SearchResponse response = searchService.search("word", null, 0, 10);

        assertThat(response.getCount()).isZero();
        assertThat(response.getData()).isEmpty();
        verifyNoInteractions(lemmaService, indexService);
    }

    // ===== 8: single page =====

    @Test
    @DisplayName("single page: relevance normalized to 1.0, SearchData fields populated")
    void search_singlePage_buildsSearchData() {
        Site site = site("https://site.example", "My Site", Status.INDEXED);
        Page page = page("/index.html", html("Test Title", "hello test page"), site);
        Lemma lemma1 = lemma("test", 1, site);
        Index idx = index(page, lemma1, 2.0f);

        when(lemmatizer.getUniqueLemmas("test")).thenReturn(Set.of("test"));
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(lemmaService.findAllByLemmaInAndSite(anySet(), eq(site))).thenReturn(List.of(lemma1));
        when(indexService.findByLemmaAndSite(lemma1, site)).thenReturn(List.of(idx));
        when(indexService.findByPageAndLemma(page, lemma1)).thenReturn(Optional.of(idx));

        SearchResponse response = searchService.search("test", site.getUrl(), 0, 10);

        assertThat(response.getCount()).isEqualTo(1);
        SearchData data = response.getData().get(0);
        assertThat(data.getSite()).isEqualTo("https://site.example");
        assertThat(data.getSiteName()).isEqualTo("My Site");
        assertThat(data.getUri()).isEqualTo("/index.html");
        assertThat(data.getTitle()).isEqualTo("Test Title");
        assertThat(data.getSnippet()).isNotBlank();
        assertThat(data.getRelevance()).isEqualTo(1.0, within(1e-6));
    }

    // ===== 9: sorting by relevance =====

    @Test
    @DisplayName("two pages: sorted by relevance desc, second normalized")
    void search_twoPages_sortedByRelevance() {
        Site site = site("https://site.example", "Site", Status.INDEXED);
        Page page1 = page("/p1", html("P1", "test one"), site);
        Page page2 = page("/p2", html("P2", "test two"), site);
        Lemma lemma1 = lemma("test", 1, site);
        Index idx1 = index(page1, lemma1, 3.0f);
        Index idx2 = index(page2, lemma1, 1.0f);

        when(lemmatizer.getUniqueLemmas("test")).thenReturn(Set.of("test"));
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(lemmaService.findAllByLemmaInAndSite(anySet(), eq(site))).thenReturn(List.of(lemma1));
        when(indexService.findByLemmaAndSite(lemma1, site)).thenReturn(List.of(idx1, idx2));
        when(indexService.findByPageAndLemma(page1, lemma1)).thenReturn(Optional.of(idx1));
        when(indexService.findByPageAndLemma(page2, lemma1)).thenReturn(Optional.of(idx2));

        SearchResponse response = searchService.search("test", site.getUrl(), 0, 10);

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getData()).hasSize(2);
        assertThat(response.getData().get(0).getUri()).isEqualTo("/p1");
        assertThat(response.getData().get(0).getRelevance()).isEqualTo(1.0, within(1e-6));
        assertThat(response.getData().get(1).getUri()).isEqualTo("/p2");
        assertThat(response.getData().get(1).getRelevance()).isCloseTo(1.0 / 3.0, within(1e-6));
    }

    // ===== 10: pagination =====

    @Test
    @DisplayName("pagination: count = total, data = single item (offset=1, limit=1)")
    void search_pagination_preservesTotalCount() {
        Site site = site("https://site.example", "Site", Status.INDEXED);
        Page page1 = page("/p1", html("P1", "test one"), site);
        Page page2 = page("/p2", html("P2", "test two"), site);
        Lemma lemma1 = lemma("test", 1, site);
        Index idx1 = index(page1, lemma1, 3.0f);
        Index idx2 = index(page2, lemma1, 1.0f);

        when(lemmatizer.getUniqueLemmas("test")).thenReturn(Set.of("test"));
        when(siteService.findByUrl(site.getUrl())).thenReturn(Optional.of(site));
        when(lemmaService.findAllByLemmaInAndSite(anySet(), eq(site))).thenReturn(List.of(lemma1));
        when(indexService.findByLemmaAndSite(lemma1, site)).thenReturn(List.of(idx1, idx2));
        when(indexService.findByPageAndLemma(page1, lemma1)).thenReturn(Optional.of(idx1));
        when(indexService.findByPageAndLemma(page2, lemma1)).thenReturn(Optional.of(idx2));

        SearchResponse response = searchService.search("test", site.getUrl(), 1, 1);

        assertThat(response.getCount()).isEqualTo(2);
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getUri()).isEqualTo("/p2");
    }

    // ===== helpers =====

    private static Site site(String url, String name, Status status) {
        Site s = new Site(name, url);
        s.setId(1L);
        s.setStatus(status);
        return s;
    }

    private static Page page(String path, String content, Site site) {
        Page p = new Page(path, 200, content, site);
        p.setId(1L);
        return p;
    }

    private static Lemma lemma(String text, int frequency, Site site) {
        Lemma l = new Lemma(text, frequency, site);
        l.setId(1L);
        return l;
    }

    private static Index index(Page page, Lemma lemma, float rank) {
        Index i = new Index(page, lemma, rank);
        i.setId(1L);
        return i;
    }

    private static String html(String title, String body) {
        return "<html><head><title>" + title + "</title></head><body>" + body + "</body></html>";
    }
}