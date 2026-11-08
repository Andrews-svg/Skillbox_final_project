package com.example.searchengine.services.crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("LinkExtractor - extractLinks() contract")
class LinkExtractorTest {

    private static final String BASE_URL = "https://site.example";
    private static final String PAGE_URL = "https://site.example/page";

    private LinkExtractor linkExtractor;

    @BeforeEach
    void setUp() {
        linkExtractor = new LinkExtractor();
    }

    private static Document parse(String html) {
        return Jsoup.parse(html, PAGE_URL);
    }

    private List<String> extract(String html) {
        return linkExtractor.extractLinks(parse(html), PAGE_URL, BASE_URL);
    }

    @Test
    @DisplayName("absolute same-domain link is included")
    void absoluteSameDomainLink_isIncluded() {
        List<String> links = extract("<a href=\"https://site.example/about\">About</a>");

        assertThat(links).containsExactly("https://site.example/about");
    }

    @Test
    @DisplayName("relative link is resolved against base URI")
    void relativeLink_isResolvedAgainstBaseUri() {
        List<String> links = extract("<a href=\"/about\">About</a>");

        assertThat(links).containsExactly("https://site.example/about");
    }

    @Test
    @DisplayName("external-domain link is excluded")
    void externalDomainLink_isExcluded() {
        List<String> links = extract(
                "<a href=\"https://other.example/x\">X</a>" +
                "<a href=\"/internal\">I</a>");

        assertThat(links).containsExactly("https://site.example/internal");
    }

    @Test
    @DisplayName("forbidden extensions (.pdf/.jpg/.css) are excluded")
    void forbiddenExtensions_areExcluded() {
        List<String> links = extract(
                "<a href=\"/file.pdf\">PDF</a>" +
                "<a href=\"/img.jpg\">JPG</a>" +
                "<a href=\"/style.css\">CSS</a>" +
                "<a href=\"/ok\">OK</a>");

        assertThat(links).containsExactly("https://site.example/ok");
    }

    @Test
    @DisplayName("non-http schemes (mailto/tel/javascript) are excluded")
    void nonHttpSchemes_areExcluded() {
        List<String> links = extract(
                "<a href=\"mailto:a@b.c\">M</a>" +
                "<a href=\"tel:+123456\">T</a>" +
                "<a href=\"javascript:void(0)\">J</a>" +
                "<a href=\"/real\">R</a>");

        assertThat(links).containsExactly("https://site.example/real");
    }

    @Test
    @DisplayName("trailing slash is stripped from result")
    void trailingSlash_isStripped() {
        List<String> links = extract("<a href=\"/sub/\">Sub</a>");

        assertThat(links).containsExactly("https://site.example/sub");
    }

    @Test
    @DisplayName("utm_* query parameters are stripped before validation")
    void utmParameters_areStripped() {
        List<String> links = extract(
                "<a href=\"/about?utm_source=x&utm_campaign=y\">About</a>");

        assertThat(links).containsExactly("https://site.example/about");
    }

    @Test
    @DisplayName("duplicate links are collapsed (HashSet)")
    void duplicateLinks_areCollapsed() {
        List<String> links = extract(
                "<a href=\"/about\">A1</a>" +
                "<a href=\"/about\">A2</a>" +
                "<a href=\"/about/\">A3</a>");

        assertThat(links).containsExactly("https://site.example/about");
    }

    @Test
    @DisplayName("data-url pagination link is included")
    void dataUrlPaginationLink_isIncluded() {
        List<String> links = extract("<div data-url=\"/page/2\">2</div>");

        assertThat(links).containsExactly("https://site.example/page/2");
    }

    @Test
    @DisplayName("query param with empty value is excluded")
    void emptyQueryParamValue_isExcluded() {
        List<String> links = extract("<a href=\"/search?filter=&sort=asc\">S</a>");

        assertThat(links).isEmpty();
    }
}