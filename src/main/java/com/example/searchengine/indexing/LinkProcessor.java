package com.example.searchengine.indexing;

import com.example.searchengine.models.Status;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.searchengine.services.PageService;
import com.example.searchengine.utils.DBSaver;
import com.example.searchengine.utils.UrlRecursiveParser;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@Transactional
public class LinkProcessor {

    private static final Logger logger = LoggerFactory.getLogger(LinkProcessor.class);
    private final UrlRecursiveParser urlRecursiveParser;
    private final PageService pageService;
    private final IndexService indexService;
    private final DBSaver dbSaver;
    private Set<String> processedLinks = new HashSet<>();
    private Document parsedDocument;

    @Autowired
    public LinkProcessor(UrlRecursiveParser urlRecursiveParser, PageService pageService,
                         IndexService indexService, DBSaver dbSaver) {
        this.urlRecursiveParser = urlRecursiveParser;
        this.pageService = pageService;
        this.indexService = indexService;
        this.dbSaver = dbSaver;
    }

    void processLinks(Site site, Document document) {
        HashSet<String> linksSet = extractLinksFromDocument(document);
        linksSet.parallelStream().forEach(link -> processLink(link, site));
        indexLinks(linksSet);
    }

    private HashSet<String> extractLinksFromDocument(Document document) {
        HashSet<String> linksSet = new HashSet<>();
        Elements links = document.select("a[href]");
        links.forEach(element -> {
            String link = element.attr("abs:href");
            linksSet.add(link);
        });

        logger.info("Извлечено {} ссылок из документа.", linksSet.size());
        return linksSet;
    }


    private void processLink(String link, Site site) {
        try {
            logger.info("Обработка ссылки для индексации: {}", link);

            Document doc = Jsoup.connect(link).get();
            String pageContent = doc.html();
            String path = urlRecursiveParser.getPathFromLink(link);

            Page page = new Page(
                    link,
                    path,
                    link,
                    site,
                    pageContent,
                    200,
                    "Page from link",
                    "Page title",
                    "Page snippet",
                    0.0f,
                    Status.INDEXED,
                    true
            );
            long pageId = pageService.validateAndSavePage(page);
            logger.info("Данные о странице сохранены с ID: {}", pageId);
        } catch (IOException e) {
            logger.warn("Ошибка при обработке ссылки {}: {}", link, e.getMessage());
        }
    }


    private void indexLinks(Set<String> links) {
        links.parallelStream().forEach(link -> {
            try {
                indexService.indexPage(link);
                dbSaver.addPagesToDatabase(link);
                logger.info("Страница успешно проиндексирована: {}", link);
            } catch (Exception e) {
                logger.error("Ошибка при индексации страницы {}: {}", link, e.getMessage(), e);
            }
        });
    }


    public boolean hasValidResults() {
        return !processedLinks.isEmpty() && parsedDocument != null;
    }
}