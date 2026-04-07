package com.example.searchengine.services;

import com.example.searchengine.models.Page;
import com.example.searchengine.models.Site;
import com.example.searchengine.repositories.PageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class PageService {

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);
    private final PageRepository pageRepository;

    public PageService(PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }


    @Transactional
    public Page save(Page page) {
        validatePage(page);
        Page saved = pageRepository.save(page);
        logger.debug("Страница сохранена: {} (сайт: {})",
                saved.getPath(), saved.getSite().getUrl());
        return saved;
    }


    @Transactional
    public List<Page> saveAll(List<Page> pages) {
        pages.forEach(this::validatePage);
        List<Page> saved = pageRepository.saveAll(pages);
        logger.debug("Сохранено {} страниц", saved.size());
        return saved;
    }


    @Transactional
    public void delete(Page page) {
        pageRepository.delete(page);
        logger.debug("Страница удалена: {}", page.getPath());
    }


    @Transactional
    public void deleteById(long pageId) {
        pageRepository.deleteById(pageId);
        logger.debug("Страница с id {} удалена", pageId);
    }


    @Transactional
    public void deleteAllBySite(Site site) {
        pageRepository.deleteBySite(site);
        logger.debug("Все страницы сайта {} удалены", site.getUrl());
    }


    @Transactional(readOnly = true)
    public Optional<Page> findByPathAndSite(String path, Site site) {
        return pageRepository.findByPathAndSite(path, site);
    }


    @Transactional(readOnly = true)
    public Optional<Page> findById(long id) {
        return pageRepository.findById(id);
    }


    @Transactional(readOnly = true)
    public List<Page> findAllBySite(Site site) {
        return pageRepository.findBySite(site);
    }


    @Transactional(readOnly = true)
    public long countBySite(Site site) {
        return pageRepository.countBySite(site);
    }


    @Transactional(readOnly = true)
    public boolean existsByPathAndSite(String path, Site site) {
        return pageRepository.existsByPathAndSite(path, site);
    }


    private void validatePage(Page page) {
        if (page.getSite() == null) {
            throw new IllegalArgumentException("Сайт страницы не может быть null");
        }
        if (page.getPath() == null || page.getPath().trim().isEmpty()) {
            throw new IllegalArgumentException("Путь страницы не может быть пустым");
        }
        if (!page.getPath().startsWith("/")) {
            throw new IllegalArgumentException("Путь страницы должен начинаться со слеша");
        }
        if (page.getContent() == null || page.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Контент страницы не может быть пустым");
        }
        if (page.getCode() < 100 || page.getCode() > 599) {
            throw new IllegalArgumentException("Некорректный HTTP код: " + page.getCode());
        }
    }


    public long getTotalPages() {
        return pageRepository.count();
    }

    public long countAll() {
        return pageRepository.count();
    }
}