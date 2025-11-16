package com.example.searchengine.services;

import com.example.searchengine.config.Site;
import com.example.searchengine.indexing.IndexService;
import com.example.searchengine.utils.Lemmatizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.models.*;
import com.example.searchengine.dto.statistics.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final IndexService indexService;
    private final PageService pageService;
    private final Lemmatizer lemmatizer;
    private final LemmaService lemmaService;
    private final SearcherService searcherService;

    @Autowired
    public SearchService(IndexService indexService,
                         PageService pageService,
                         Lemmatizer lemmatizer, LemmaService lemmaService,
                         SearcherService searcherService) {
        this.indexService = indexService;
        this.pageService = pageService;
        this.lemmatizer = lemmatizer;
        this.lemmaService = lemmaService;
        this.searcherService = searcherService;
    }



    public SearchResult search(
            String query, String siteURL, int offset, int limit) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'query' parameter cannot be null or empty");
        }
        if (siteURL == null || siteURL.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "The 'siteURL' parameter cannot be null or empty");
        }
        logger.info("Starting search with query: '{}' and siteURL: '{}'", query, siteURL);

        ArrayList<Data> dataList = searchStringToDataArray(query, siteURL);
        Integer totalResults = (int) dataList.size();

        List<Data> paginatedResults = dataList.stream()
                .skip(offset)
                .limit(limit)
                .toList();

        return new SearchResult(true, totalResults, paginatedResults);
    }


    public ArrayList<Data> searchStringToDataArray(String searchInput,
                                                   String siteURL) {
        logger.info("Starting search with input: '{}' and siteURL: '{}'",
                searchInput, siteURL);

        try {
            ArrayList<Lemma> sortedArray =
                    inputToLemmasSortedArrayWithoutTooFrequentLemmas(searchInput, siteURL);
            if (sortedArray.isEmpty()) {
                logger.warn("No valid search terms were extracted from the input.");
                return new ArrayList<>();
            }
            List<Index> leastFrequentLemmaIndexes =
                    getLeastFrequentLemmaIndexes(sortedArray);
            return lemmaIndexesToData(leastFrequentLemmaIndexes, sortedArray);
        } catch (RuntimeException e) {
            logger.error("An error occurred during the search process: {}",
                    e.getMessage(), e);
            return new ArrayList<>();
        }
    }


    public ArrayList<Data> lemmaIndexesToData(List<Index> leastFrequentLemmaIndexes,
                                              ArrayList<Lemma> sortedArray) {
        ArrayList<Data> resultList = new ArrayList<>();
        leastFrequentLemmaIndexes.forEach(index -> {
            try {
                Page page = index.getPage();
                Site site = page.getSite();
                Lemma lemma = index.getLemma();

                Data data = new Data(
                        null,
                        site,
                        site.getName(),
                        page.getPath(),
                        findPageTitle(page.getContent()),
                        findPageSnippet(page.getContent(), lemma.getLemma()),
                        0
                );

                for (Lemma currentLemma : sortedArray) {
                    Integer lemmaIndexId = currentLemma.getId();
                    if (indexService.checkIfIndexExists(page.getId(),
                            lemmaIndexId)) {
                        data.setRelevance(data.getRelevance() + 1);
                    }
                }
                resultList.add(data);
            } catch (Exception e) {
                logger.error("Error processing lemma index to data: {}",
                        e.getMessage(), e);
            }
        });
        resultList.sort(Comparator.comparingDouble(Data::getRelevance).reversed());
        return resultList;
    }

    private List<Index> getLeastFrequentLemmaIndexes(List<Lemma> lemmaList) {
        if (lemmaList.isEmpty()) {
            return new ArrayList<>();
        }
        Lemma leastFrequentLemma = findLeastFrequentLemma(lemmaList);
        return new ArrayList<>(indexService.findByLemmaId(leastFrequentLemma.getId()));
    }

    private Lemma findLeastFrequentLemma(List<Lemma> lemmaList) {
        return lemmaList.stream()
                .min(Comparator.comparingDouble(Lemma::getFrequency))
                .orElse(null);
    }

    public String findPageTitle(String htmlContent) {
        Document doc = Jsoup.parse(htmlContent);
        Element titleElement = doc.selectFirst("title");
        return titleElement != null ? titleElement.text() : "";
    }

    public String findPageSnippet(String pageContent, String leastFrequentLemma) {
        try {
            String cleanContent = Jsoup.clean(pageContent, Safelist.none());
            String[] words = cleanContent.split("\\s+");

            for (int i = 0; i < words.length; i++) {
                if (words[i].contains(leastFrequentLemma)) {
                    int start = Math.max(i - 5, 0);
                    int end = Math.min(i + 5, words.length);
                    StringBuilder snippet = new StringBuilder();
                    for (int j = start; j < end; j++) {
                        snippet.append(words[j]).append(' ');
                    }
                    return snippet.toString().trim();
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка при поиске фрагмента страницы: {}",
                    e.getMessage(), e);
        }
        return "...";
    }


    public ArrayList<Lemma> inputToLemmasSortedArrayWithoutTooFrequentLemmas(String input, String siteURL) {
        List<String> basicForms = lemmatizer.getBasicFormsFromString(input);
        int totalPagesCount = pageService.countPages();
        double frequencyThreshold = calculateDynamicFrequencyThreshold(totalPagesCount);
        int tooFrequentCoefficient = (int)(totalPagesCount * frequencyThreshold);
        ArrayList<Lemma> lemmasSortedList = new ArrayList<>();
        for (String form : basicForms) {
            List<Lemma> listFromDB = new ArrayList<>();
            if (!siteURL.isEmpty()) {
                List<Site> sites = searcherService.findByPartialUrl(siteURL);
                Optional<Site> matchingSite = sites.stream()
                        .filter(site -> site.getUrl().equals(siteURL))
                        .findFirst();
                if (matchingSite.isPresent()) {
                    Site site = matchingSite.get();
                    Optional<Lemma> lemmaOpt = lemmaService.findByBaseFormAndSiteId(form, site.getId());
                    lemmaOpt.ifPresent(listFromDB::add);
                } else {
                    logger.warn("Сайт с URL {} не найден", siteURL);
                    continue;
                }
            } else {
                listFromDB = lemmaService.findLemmaByName(form);
            }
            for (Lemma l : listFromDB) {
                if (l.getFrequency() <= tooFrequentCoefficient && l.getFrequency() > 0) {
                    lemmasSortedList.add(l);
                }
            }
        }
        Collections.sort(lemmasSortedList);
        return lemmasSortedList;
    }


    private double calculateDynamicFrequencyThreshold(int totalPagesCount) {
        return Math.log(totalPagesCount) / totalPagesCount;
    }
}


