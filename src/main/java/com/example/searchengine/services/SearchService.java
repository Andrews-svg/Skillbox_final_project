package com.example.searchengine.services;

import com.example.searchengine.models.*;
import com.example.searchengine.dto.search.SearchData;
import com.example.searchengine.dto.search.SearchResponse;
import com.example.searchengine.services.indexing.IndexService;
import com.example.searchengine.utils.Lemmatizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);
    private static final int SNIPPET_WORDS_BEFORE = 7;
    private static final int SNIPPET_WORDS_AFTER = 7;
    private static final int MAX_SNIPPET_LENGTH = 300;

    private final LemmaService lemmaService;
    private final IndexService indexService;
    private final SiteService siteService;
    private final Lemmatizer lemmatizer;

    public SearchService(LemmaService lemmaService,
                         IndexService indexService,
                         SiteService siteService,
                         Lemmatizer lemmatizer) {
        this.lemmaService = lemmaService;
        this.indexService = indexService;
        this.siteService = siteService;
        this.lemmatizer = lemmatizer;
    }


    public SearchResponse search(String query, String siteUrl, int offset, int limit) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Задан пустой поисковый запрос");
        }
        logger.info("Поисковый запрос: '{}', сайт: {}", query, siteUrl != null ? siteUrl : "все");
        Set<String> lemmaSet = lemmatizer.getUniqueLemmas(query);
        if (lemmaSet.isEmpty()) {
            logger.info("Не удалось извлечь леммы из запроса: {}", query);
            return SearchResponse.success(0, Collections.emptyList());
        }
        List<Site> sites = getSitesForSearch(siteUrl);
        if (sites.isEmpty()) {
            return SearchResponse.success(0, Collections.emptyList());
        }
        List<SearchData> allResults = new ArrayList<>();
        for (Site site : sites) {
            allResults.addAll(searchInSite(lemmaSet, site));
        }
        allResults.sort((a, b) ->
                Double.compare(b.getRelevance(), a.getRelevance()));
        int total = allResults.size();
        List<SearchData> paginated = allResults.stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
        logger.info("Найдено результатов: {}, показано: {}", total, paginated.size());
        return SearchResponse.success(total, paginated);
    }


    private List<Site> getSitesForSearch(String siteUrl) {
        if (siteUrl != null && !siteUrl.isEmpty()) {
            Optional<Site> site = siteService.findByUrl(siteUrl);
            if (site.isEmpty()) {
                logger.warn("Сайт с URL {} не найден", siteUrl);
                return Collections.emptyList();
            }
            if (site.get().getStatus() != Status.INDEXED) {
                logger.warn("Сайт {} еще не проиндексирован (статус: {})",
                        siteUrl, site.get().getStatus());
                return Collections.emptyList();
            }
            return List.of(site.get());
        } else {
            return siteService.findAll().stream()
                    .filter(s -> s.getStatus() == Status.INDEXED)
                    .collect(Collectors.toList());
        }
    }


    private List<SearchData> searchInSite(Set<String> lemmaSet, Site site) {
        List<Lemma> lemmas = lemmaService.findAllByLemmaInAndSite(lemmaSet, site);
        if (lemmas.isEmpty()) {
            return Collections.emptyList();
        }
        List<Lemma> sortedLemmas = lemmas.stream()
                .sorted(Comparator.comparingInt(Lemma::getFrequency))
                .collect(Collectors.toList());
        Lemma firstLemma = sortedLemmas.get(0);
        List<Index> indexes = indexService.findByLemmaAndSite(firstLemma, site);
        Set<Page> pages = indexes.stream()
                .map(Index::getPage)
                .collect(Collectors.toSet());
        for (int i = 1; i < sortedLemmas.size(); i++) {
            Lemma lemma = sortedLemmas.get(i);
            Set<Page> pagesWithLemma = indexService.findByLemmaAndSite(lemma, site).stream()
                    .map(Index::getPage)
                    .collect(Collectors.toSet());
            pages.retainAll(pagesWithLemma);
            if (pages.isEmpty()) break;
        }
        Map<Page, Double> absoluteRelevance = new HashMap<>();
        for (Page page : pages) {
            double totalRank = 0.0;
            for (Lemma lemma : sortedLemmas) {
                Optional<Index> index = indexService.findByPageAndLemma(page, lemma);
                if (index.isPresent()) {
                    totalRank += index.get().getRank();
                }
            }
            absoluteRelevance.put(page, totalRank);
        }
        double maxRelevance = absoluteRelevance.values().stream()
                .max(Double::compare)
                .orElse(1.0);
        List<SearchData> results = new ArrayList<>();
        for (Page page : pages) {
            double relevance = absoluteRelevance.get(page) / maxRelevance;
            SearchData data = new SearchData();
            data.setSite(site.getUrl());
            data.setSiteName(site.getName());
            data.setUri(page.getPath());
            data.setTitle(extractTitle(page.getContent()));
            data.setSnippet(generateSnippet(page.getContent(), sortedLemmas));
            data.setRelevance(relevance);
            results.add(data);
        }
        return results;
    }


    private String extractTitle(String html) {
        try {
            Document doc = Jsoup.parse(html);
            String title = doc.title();
            return !title.isEmpty() ? title : "Без заголовка";
        } catch (Exception e) {
            return "Без заголовка";
        }
    }


    private String generateSnippet(String html, List<Lemma> lemmas) {
        try {
            Document doc = Jsoup.parse(html);
            String text = doc.body().text();
            String[] words = text.replaceAll("\\s+", " ").split(" ");
            Set<String> lemmaTexts = lemmas.stream()
                    .map(l -> l.getLemma().toLowerCase())
                    .collect(Collectors.toSet());
            int bestPos = findBestSnippetPosition(words, lemmaTexts);
            if (bestPos == -1) {
                return text.length() > MAX_SNIPPET_LENGTH
                        ? text.substring(0, MAX_SNIPPET_LENGTH) + "..."
                        : text;
            }
            int start = Math.max(0, bestPos - SNIPPET_WORDS_BEFORE);
            int end = Math.min(words.length, bestPos + SNIPPET_WORDS_AFTER + 1);
            StringBuilder snippet = new StringBuilder();
            if (start > 0) snippet.append("... ");
            for (int i = start; i < end; i++) {
                String word = words[i];
                String wordLower = word.toLowerCase();
                boolean match = lemmaTexts.stream()
                        .anyMatch(wordLower::contains);
                if (match) {
                    snippet.append("<b>").append(word).append("</b> ");
                } else {
                    snippet.append(word).append(" ");
                }
            }
            if (end < words.length) snippet.append("...");
            return snippet.toString().trim();
        } catch (Exception e) {
            return "Содержимое страницы недоступно...";
        }
    }


    private int findBestSnippetPosition(String[] words, Set<String> lemmaTexts) {
        int bestPos = -1;
        int maxMatches = 0;
        for (int i = 0; i < words.length; i++) {
            int matches = 0;
            for (int j = i; j < Math.min(words.length, i + SNIPPET_WORDS_AFTER * 2); j++) {
                String wordLower = words[j].toLowerCase();
                if (lemmaTexts.stream().anyMatch(wordLower::contains)) {
                    matches++;
                }
            }
            if (matches > maxMatches) {
                maxMatches = matches;
                bestPos = i;
            }
            if (maxMatches >= 2) break;
        }
        return bestPos;
    }
}