package com.example.searchengine.services;

import com.example.searchengine.exceptions.InvalidSiteException;

import java.io.IOException;
import java.util.Set;

public interface DataSaver {
    void saveData(String url, String title, Set<String> outLinksSet) throws IOException, InvalidSiteException, SiteService.InvalidSiteException;
}