package com.example.searchengine.services.crawler;

import com.example.searchengine.models.Page;
import org.jsoup.nodes.Document;

/**
 * Результат обработки одной страницы: сущность Page (для БД) + Document (для извлечения ссылок).
 * Document НЕ сохраняется в БД — используется только во время обхода.
 */
public record ProcessedPage(Page page, Document document) {
}