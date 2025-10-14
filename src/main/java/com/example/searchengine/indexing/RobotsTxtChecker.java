package com.example.searchengine.indexing;

import com.example.searchengine.services.SearcherService;
import jakarta.transaction.Transactional;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.example.searchengine.models.Site;
import com.example.searchengine.services.SiteService;
import com.example.searchengine.utils.DBSaver;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class RobotsTxtChecker {

    private final SiteService siteService;
    private final SearcherService searchService;
    private final DBSaver dbSaver;

    private static final Logger logger =
            LoggerFactory.getLogger(RobotsTxtChecker.class);

    @Autowired
    public RobotsTxtChecker(SiteService siteService,
                            SearcherService searchService, DBSaver dbSaver) {
        this.siteService = siteService;
        this.searchService = searchService;
        this.dbSaver = dbSaver;
    }

    @Async
    public CompletableFuture<Boolean> isSiteIndexableAsync(Site site) {
        if (site == null) {
            logger.error("Передан null объект Site");
            return CompletableFuture.completedFuture(false);
        }

        logger.debug("Начало проверки сайта с ID: {}", site.getId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                String robotsTxtUrl = site.getUrl() + "/robots.txt";
                Response response = Jsoup.connect(robotsTxtUrl)
                        .header("Accept-Charset", StandardCharsets.UTF_8.name())
                        .ignoreHttpErrors(true)
                        .execute();

                int httpStatusCode = response.statusCode();
                if (httpStatusCode != 200) {
                    logger.warn("HTTP Status Code: {}, robots.txt недоступен.", httpStatusCode);
                    return true;
                }

                String robotsContent = response.body();
                logger.info("Содержимое robots.txt для сайта с ID " +
                        "{}: {}", site.getId(), robotsContent);
                Pattern disallowPattern = Pattern.compile("^\\s*Disallow:\\s*(.*)",
                        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
                Matcher matcher = disallowPattern.matcher(robotsContent);
                boolean isAllowed = true;
                while (matcher.find()) {
                    String path = URLDecoder.decode(matcher.group(1).trim(),
                            StandardCharsets.UTF_8);

                    if ("/".equals(path) || site.getUrl().endsWith(path)) {
                        logger.warn("Сайт с ID: {} " +
                                "запрещает индексацию через robots.txt", site.getId());
                        isAllowed = false;
                        break;
                    }
                }

                if (isAllowed && !site.getUrl().isBlank()) {
                    CompletableFuture<Response> htmlResponseFuture =
                            CompletableFuture.supplyAsync(
                            () -> {
                                try {
                                    return Jsoup.connect(site.getUrl())
                                            .header("Accept-Charset",
                                                    StandardCharsets.UTF_8.name())
                                            .ignoreHttpErrors(true)
                                            .execute();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                    );

                    Response htmlResponse = htmlResponseFuture.join();
                    int htmlStatusCode = htmlResponse.statusCode();
                    if (htmlStatusCode != 200) {
                        logger.warn("HTTP Status Code: {}, " +
                                "главная страница недоступна.", htmlStatusCode);
                        return true;
                    }

                    String mainPageContent = htmlResponse.body();
                    if (!mainPageContent.isEmpty()) {
                        Document doc = Jsoup.parse(mainPageContent);
                        Elements metaTags = doc.select("meta[name=robots]");

                        if (!metaTags.isEmpty()) {
                            for (Element metaTag : metaTags) {
                                if (metaTag.attr("content").contains("noindex")) {
                                    logger.warn("Сайт с ID: " +
                                            "{} содержит noindex в мета-тегах",
                                            site.getId());
                                    isAllowed = false;
                                    break;
                                }
                            }
                        }
                    }
                }

                return isAllowed;
            } catch (IOException e) {
                logger.error("Ошибка загрузки robots.txt " +
                        "или главной страницы: {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });
    }

    public boolean checkIfSiteIsIndexed(String searchURL) {
        String siteHost = "%" + dbSaver.getHostFromLink(searchURL) + "%";
        return searchService.checkIfSiteWithNameExists(siteHost);
    }
}