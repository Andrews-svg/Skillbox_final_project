package com.example.searchengine.services;


import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Component
@Scope("singleton")
public class CrawlingContext {


    private final ConcurrentHashMap<Long, Queue<String>> paginationLinks = new ConcurrentHashMap<>();


    public void addPaginationLinks(Long siteId, List<String> links) {
        if (links == null || links.isEmpty()) return;

        paginationLinks.computeIfAbsent(siteId, k -> new ConcurrentLinkedQueue<>())
                .addAll(links);
    }


    public String pollPaginationLink(Long siteId) {
        Queue<String> queue = paginationLinks.get(siteId);
        return queue != null ? queue.poll() : null;
    }


    public boolean hasPaginationLinks(Long siteId) {
        Queue<String> queue = paginationLinks.get(siteId);
        return queue != null && !queue.isEmpty();
    }


    public void clearForSite(Long siteId) {
        paginationLinks.remove(siteId);
    }


    public void clearAll() {
        paginationLinks.clear();
    }
}