package com.example.searchengine.services;

import com.example.searchengine.config.Site;
import com.example.searchengine.repository.SiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SearcherService {

    private static final Logger logger =
            LoggerFactory.getLogger(SearcherService.class);

    private final SiteRepository siteRepository;

    @Autowired
    public SearcherService(SiteRepository siteRepository) {
        this.siteRepository = siteRepository;
    }


    public List<Site> findByPartialUrl(String partialUrl) {
        return siteRepository.findByUrlContaining(partialUrl);
    }


    public boolean checkIfSiteWithNameExists(String siteUrl) {
        return siteRepository.existsByUrl(siteUrl);
    }
}
