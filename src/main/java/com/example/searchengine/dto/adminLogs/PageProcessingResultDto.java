package com.example.searchengine.dto;

import java.util.HashSet;
import java.util.Set;

public class PageProcessingResultDto {
    private final boolean success;
    private final String url;
    private final String title;
    private final String content;
    private final Set<String> links;
    private final int statusCode;
    private final String error;

    private PageProcessingResultDto(boolean success, String url, String title, String content,
                                    Set<String> links, int statusCode, String error) {
        this.success = success;
        this.url = url;
        this.title = title;
        this.content = content;
        this.links = links != null ? links : new HashSet<>();
        this.statusCode = statusCode;
        this.error = error;
    }

    public static PageProcessingResultDto success(String url, String title, String content, Set<String> links) {
        return new PageProcessingResultDto(true, url, title, content, links, 200, null);
    }

    public static PageProcessingResultDto error(String url, int statusCode, String error) {
        return new PageProcessingResultDto(false, url, null, null, null, statusCode, error);
    }

    public boolean isSuccess() { return success; }
    public String getUrl() { return url; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public Set<String> getLinks() { return links; }
    public int getStatusCode() { return statusCode; }
    public String getError() { return error; }
}