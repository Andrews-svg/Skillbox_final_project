package com.example.searchengine.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;
import java.io.IOException;


@Component
public class JsoupWrapper {
    public Document connect(String url, String userAgent, String referrer) throws IOException {
        return Jsoup.connect(url)
                .userAgent(userAgent)
                .referrer(referrer)
                .timeout(10000)
                .get();
    }
}