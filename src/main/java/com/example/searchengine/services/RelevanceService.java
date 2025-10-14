package com.example.searchengine.services;

import com.example.searchengine.models.Page;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RelevanceService {

    public double calculateRelevance(Page page) {
        Set<String> keywords = extractKeywords(page.getContent());
        int keywordFrequency = countKeywordOccurrences(keywords, page.getContent());
        int pageLength = page.getContent().length();
        double lengthFactor = 1 / Math.log(Math.max(1, pageLength));
        return keywordFrequency * lengthFactor;
    }

    private Set<String> extractKeywords(String content) {
        return Arrays.stream(content.split("\\W+"))
                .filter(word -> word.length() >= 3)
                .collect(Collectors.toSet());
    }

    private int countKeywordOccurrences(Set<String> keywords, String content) {
        int count = 0;
        for (String word : content.split("\\W+")) {
            if (keywords.contains(word)) {
                count++;
            }
        }
        return count;
    }
}