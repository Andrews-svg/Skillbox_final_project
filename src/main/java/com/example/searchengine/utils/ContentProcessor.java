package com.example.searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@Component
public class ContentProcessor {

    private final Lemmatizer lemmatizer;

    public ContentProcessor(Lemmatizer lemmatizer) {
        this.lemmatizer = lemmatizer;
    }


    public void processContent(String content, Map<String, Float> mapTitle, Map<String, Float> mapBody) {
        Document doc = Jsoup.parse(content);
        Elements titleElements = doc.select("title");
        Elements bodyElements = doc.select("body");

        if (!titleElements.isEmpty()) {
            String titleText = Objects.requireNonNull(titleElements.first()).text();
            Map<String, Integer> titleFreqMap = lemmatizer.lemmasFrequencyMapFromString(titleText);
            titleFreqMap.forEach((key, value) -> mapTitle.put(key, value.floatValue()));
        }

        if (!bodyElements.isEmpty()) {
            String bodyText = Objects.requireNonNull(bodyElements.first()).text();
            Map<String, Integer> bodyFreqMap = lemmatizer.lemmasFrequencyMapFromString(bodyText);
            bodyFreqMap.forEach((key, value) -> mapBody.put(key, value.floatValue() * 0.8f));
        }
    }


    public Map<String, Float> combineMaps(Map<String, Float> mapTitle,
                                          Map<String, Float> mapBody) {
        Map<String, Float> combinedMap = new HashMap<>(mapBody);
        mapTitle.forEach((k, v) -> combinedMap.merge(k, v, Float::sum));
        return combinedMap;
    }


    public Map<String, Float> combineTwoMaps(Map<String, Float> mapTitle,
                                             Map<String, Float> mapBody) {
        mapTitle.forEach((k, v) -> mapBody.merge(k, v, Float::sum));
        return mapBody;
    }


    public double computeRelevance(String content, String query) {
        int occurrences = countOccurrences(content, query);
        return (double) occurrences / content.length();
    }


    private int countOccurrences(String text, String substring) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(substring, index)) != -1) {
            count++;
            index += substring.length();
        }
        return count;
    }
}