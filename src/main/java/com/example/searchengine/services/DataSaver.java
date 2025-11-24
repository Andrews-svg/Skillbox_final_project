package com.example.searchengine.services;

import java.io.IOException;
import java.util.Set;

public interface DataSaver {
    void saveData(String url, String title, Set<String> outLinksSet) throws IOException;
}