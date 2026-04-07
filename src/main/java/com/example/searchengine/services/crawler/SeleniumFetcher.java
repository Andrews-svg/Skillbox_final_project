package com.example.searchengine.services;

import com.example.searchengine.config.CrawlerConfig;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;

@Service
public class SeleniumFetcher {
    private static final Logger logger = LoggerFactory.getLogger(SeleniumFetcher.class);
    private final CrawlerConfig crawlerConfig;


    public SeleniumFetcher(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
    }


    public Document fetchWithBrowser(String url) {
        logger.debug("🚀 Запуск Chrome для: {}", url);
        ChromeOptions options = new ChromeOptions();
        System.setProperty("webdriver.chrome.driver", "D:\\My ProGramms\\chromedriver-win64\\chromedriver.exe");
        options.addArguments("--headless");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-images");
        options.addArguments("--blink-settings=imagesEnabled=false");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        options.addArguments("--lang=ru-RU");
        options.addArguments("--accept-lang=ru-RU,ru;q=0.9");
        options.setPageLoadTimeout(Duration.ofSeconds(10));
        WebDriver driver = null;
        try {
            long startTime = System.currentTimeMillis();
            logger.debug("⏱ Создание ChromeDriver...");
            driver = new ChromeDriver(options);
            logger.debug("✅ ChromeDriver создан за {} мс", System.currentTimeMillis() - startTime);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(10));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(5));
            logger.debug("⏱ Загрузка страницы...");
            driver.get(url);
            logger.debug("⏱ Ожидание JS...");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            logger.debug("⏱ Получение HTML...");
            String pageSource = driver.getPageSource();
            logger.debug("✅ Получен HTML, размер: {} байт за {} мс", pageSource.length(), System.currentTimeMillis() - startTime);
            return Jsoup.parse(pageSource);
        } catch (Exception e) {
            logger.error("❌ Ошибка Chrome: {}", e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }
    }


    public boolean shouldUseBrowser(String url) {
        List<String> jsSites = crawlerConfig.getJsEnabledSites();
        if (jsSites == null || jsSites.isEmpty()) {
            return false;
        }
        return jsSites.stream().anyMatch(url::contains);
    }
}