package com.example.searchengine.services.crawler;

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
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Service
public class SeleniumFetcher {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumFetcher.class);
    private final CrawlerConfig crawlerConfig;


    private static final Set<String> JS_HEAVY_SITES = Set.of(
            "ozon.ru",
            "wildberries.ru",
            "ozon.by",
            "wildberries.by",
            "market.yandex.ru",
            "aliexpress.ru",
            "sbermegamarket.ru",
            "citilink.ru",
            "dns-shop.ru",
            "mvideo.ru",
            "eldorado.ru",
            "ozon.uz",
            "wildberries.kz",
            "lamoda.ru",
            "sportmaster.ru",
            "detmir.ru"
    );

    private static final int MAX_CONCURRENT_BROWSERS_SINGLE = 2;
    private static final int MAX_CONCURRENT_BROWSERS_MULTI = 3;
    private final Semaphore browserSemaphore;


    public SeleniumFetcher(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
        int maxBrowsers = crawlerConfig.getCurrentMode().contains("МУЛЬТИ")
                ? MAX_CONCURRENT_BROWSERS_MULTI
                : MAX_CONCURRENT_BROWSERS_SINGLE;
        this.browserSemaphore = new Semaphore(maxBrowsers, true);
        logger.info("🔄 SeleniumFetcher: максимум {} параллельных браузеров", maxBrowsers);
    }


    public Document fetchWithBrowser(String url) {
        logger.debug("🚀 Ожидание доступа к браузеру для: {}", url);
        boolean acquired = false;
        WebDriver driver = null;
        try {
            acquired = browserSemaphore.tryAcquire(30, TimeUnit.SECONDS);
            if (!acquired) {
                logger.warn("⏱ Таймаут ожидания браузера (30 сек) для {}", url);
                return null;
            }
            logger.debug("✅ Доступ получен, запуск Chrome для: {}", url);
            ChromeOptions options = new ChromeOptions();
            System.setProperty("webdriver.chrome.driver", "D:\\My ProGramms\\chromedriver-win64\\chromedriver.exe");
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-images");
            options.addArguments("--blink-settings=imagesEnabled=false");
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            options.addArguments("--lang=ru-RU");
            options.addArguments("--accept-lang=ru-RU,ru;q=0.9");
            int timeoutSeconds = crawlerConfig.getTimeout() / 1000;
            options.setPageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
            long startTime = System.currentTimeMillis();
            logger.debug("⏱ Создание ChromeDriver...");
            driver = new ChromeDriver(options);
            logger.debug("✅ ChromeDriver создан за {} мс", System.currentTimeMillis() - startTime);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(timeoutSeconds));
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(5));
            logger.debug("⏱ Загрузка страницы: {}", url);
            driver.get(url);
            int delay = crawlerConfig.getRandomDelay();
            logger.debug("⏱ Ожидание JS {} мс...", delay);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("⏹ Ожидание прервано для {}", url);
                return null;
            }
            logger.debug("⏱ Получение HTML...");
            String pageSource = driver.getPageSource();
            logger.debug("✅ Получен HTML, размер: {} байт за {} мс",
                    pageSource.length(), System.currentTimeMillis() - startTime);
            return Jsoup.parse(pageSource);
        } catch (Exception e) {
            logger.error("❌ Ошибка Selenium для {}: {}", url, e.getMessage());
            return null;
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception e) {
                    logger.error("❌ Ошибка при закрытии Chrome: {}", e.getMessage());
                }
            }
            if (acquired) {
                browserSemaphore.release();
                logger.debug("🔓 Семафор освобождён для {}", url);
            }
        }
    }


    public boolean shouldUseBrowser(String url) {
        for (String site : JS_HEAVY_SITES) {
            if (url.contains(site)) {
                logger.debug("🔧 SELENIUM для {}", url);
                return true;
            }
        }
        logger.debug("📄 JSOUP для {}", url);
        return false;
    }


    public void updateConcurrencyLimit() {
        int newLimit = crawlerConfig.getCurrentMode().contains("МУЛЬТИ")
                ? MAX_CONCURRENT_BROWSERS_MULTI
                : MAX_CONCURRENT_BROWSERS_SINGLE;
        logger.info("⚠️ Текущий лимит браузеров: {} (режим: {})",
                newLimit, crawlerConfig.getCurrentMode());
    }
}