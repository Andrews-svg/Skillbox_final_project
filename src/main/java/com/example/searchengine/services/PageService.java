package com.example.searchengine.services;

import com.example.searchengine.models.Site;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.searchengine.models.Page;
import com.example.searchengine.repository.PageRepository;

import java.util.Optional;

@Service
public class PageService {

    private final PageRepository pageRepository;
    private final SiteService siteService;

    private static final Logger logger =
            LoggerFactory.getLogger(PageService.class);

    @Autowired
    public PageService(PageRepository pageRepository, SiteService siteService) {
        this.pageRepository = pageRepository;
        this.siteService = siteService;
    }

    @Transactional
    public Page savePage(Page page) {
        if (page.getCodeOptional().isEmpty()) {
            throw new IllegalArgumentException("Код страницы не указан!");
        }

        if (page.getContent() == null || page.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Содержание страницы не указано!");
        }

        if (page.getSite() == null) {
            Site determinedSite = siteService.determineSiteForPage(page.getUri());
            if (determinedSite == null) {
                throw new IllegalArgumentException("Не удалось определить сайт для страницы.");
            }
            page.setSite(determinedSite);
        }

        Optional<Page> existingPageOpt = pageRepository.findByUri(page.getUri());

        if (existingPageOpt.isPresent()) {
            Page existingPage = existingPageOpt.get();
            updateExistingPage(existingPage, page);
            return existingPage;
        } else {
            validateNewPage(page);
            return pageRepository.save(page);
        }
    }


    private void updateExistingPage(Page existingPage, Page updatedPage) {
        existingPage.setContent(updatedPage.getContent());
        existingPage.setStatus(updatedPage.getStatus());
        existingPage.setRelevance(updatedPage.getRelevance());
        existingPage.setSnippet(updatedPage.getSnippet());
        existingPage.setTitle(updatedPage.getTitle());
    }

    private void validateNewPage(Page page) {
        if (pageRepository.existsByUri(page.getUri())) {
            throw new IllegalArgumentException("Страница с таким URI уже существует!");
        }

    }


    public Long validateAndSavePage(Page page) {
        validatePage(page);
        pageRepository.save(page);
        return page.getId();
    }


    private void validatePage(Page page) {
        if (page == null || page.getPath() == null || page.getPath().isEmpty() ||
                page.getUrl() == null || page.getUrl().isEmpty()) {
            throw new IllegalArgumentException("Page, its path and URL must not be null or empty");
        }
    }

    @Transactional(readOnly = true)
    public Optional<Page> findByPath(String path) {
        return pageRepository.findByPath(path);
    }

    public long countPages() {
        return pageRepository.count();
    }

    public Optional<Page> findPage(Long id) {
        return pageRepository.findById(id);
    }

    public Optional<Page> findById(long pageId) {
        return pageRepository.findById(pageId);
    }

    public Optional<String> findUrlById(Long id) {
        return pageRepository.findById(id)
                .map(Page::getUrl);
    }

    @Transactional
    public void deletePage(Page page) {
        pageRepository.delete(page);
    }

    @Transactional
    public void deleteAllPages() {
        pageRepository.deleteAll();
    }



    @Transactional(readOnly = true)
    public long getTotalPages() {
        return pageRepository.count();
    }

}