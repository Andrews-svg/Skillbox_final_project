package com.example.searchengine.controllers.web;

import com.example.searchengine.exceptions.ErrorMessages;
import com.example.searchengine.indexing.IndexingService;
import com.example.searchengine.indexing.PageManager;
import com.example.searchengine.models.Status;
import com.example.searchengine.services.PageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/indexing")
public class IndexingWebController {

    private final IndexingService indexingService;
    private final PageService pageService;
    private final PageManager pageManager;
    private final IndexService indexService;

    private static final Logger logger = LoggerFactory.getLogger(IndexingWebController.class);

    public IndexingWebController(IndexingService indexingService,
                                 PageService pageService, PageManager pageManager,
                                 IndexService indexService) {
        this.indexingService = indexingService;
        this.pageService = pageService;
        this.pageManager = pageManager;
        this.indexService = indexService;
    }


    private void handleError(RedirectAttributes redirectAttrs, Exception e) {
        logger.error("Ошибка при обработке запроса: {}", e.getMessage(), e);
        redirectAttrs.addFlashAttribute("error",
                String.format(ErrorMessages.INDEXING_ERROR_MESSAGE_TEMPLATE, e.getMessage()));
    }


    @PostMapping("/toggleIndexing")
    public String toggleIndexing(Model model,
                                 RedirectAttributes redirectAttrs,
                                 @RequestParam Long pageId,
                                 @RequestParam boolean isLemma) {
        String indexingStatusStr = pageManager.getIndexingStatusById(pageId, isLemma);
        Status indexingStatus;
        try {
            indexingStatus = Status.valueOf(indexingStatusStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            redirectAttrs.addFlashAttribute("error",
                    "Недопустимый статус индексации.");
            return "redirect:/";
        }
        if (indexingStatus.equals(Status.INDEXING)) {
            stopIndexing(model, redirectAttrs, pageId);
        } else {
            startIndexing(model, redirectAttrs, pageId, isLemma);
        }
        return "redirect:/";
    }


    @PostMapping("/startIndexing")
    public String startIndexing(Model model,
                                RedirectAttributes redirectAttrs,
                                @RequestParam Long id,
                                @RequestParam boolean isLemma) {
        try {
            indexingService.startIndexing(id, isLemma);
            model.addAttribute("response",
                    "Индексация начата для всех сайтов");
            logger.info("Успешно начата индексация для всех сайтов");
        } catch (Exception e) {
            logger.error("Ошибка при попытке начать индексацию: {}", e.getMessage(), e);
            redirectAttrs.addFlashAttribute("error",
                    "Ошибка при начале индексации: " + e.getMessage());
        }
        return "redirect:/";
    }


    @PostMapping("/stopIndexing")
    public void stopIndexing(Model model,
                             RedirectAttributes redirectAttrs,
                             @RequestParam Long pageId) {
        try {
            indexingService.stopIndexing(pageId);
            model.addAttribute("response",
                    "Индексация успешно остановлена");
            logger.info("Успешно остановлена индексация для страницы с ID={}", pageId);
        } catch (Exception e) {
            logger.error("Ошибка при остановке индексации: {}", e.getMessage(), e);
            redirectAttrs.addFlashAttribute("error",
                    "Ошибка при остановке индексации: " + e.getMessage());
        }
    }


    @PostMapping("/indexPage")
    public String indexPage(@RequestParam String url,
                            Model model,
                            RedirectAttributes redirectAttrs) {
        try {

            indexService.indexPage(url);
            model.addAttribute("response", "Запущена индексация страницы");
        } catch (Exception e) {
            logger.error("Ошибка при индексации страницы '{}': {}", url, e.getMessage());
            redirectAttrs.addFlashAttribute("error",
                    "Ошибка при индексации страницы: " + url);
        }
        return "redirect:/";
    }


    @GetMapping("/isIndexing")
    public String isIndexing(Model model,
                             RedirectAttributes redirectAttrs) {
        try {
            boolean isIndexing = indexingService.isIndexing();
            redirectAttrs.addFlashAttribute("isIndexing", isIndexing);
        } catch (Exception e) {
            logger.error("Ошибка при проверке статуса индексации: {}", e.getMessage());
            redirectAttrs.addFlashAttribute("errorMessage",
                    "Ошибка при получении статуса индексации");
        }
        return "redirect:/";
    }


    @PostMapping("/startIndexingAllSites")
    public String startIndexingAllSites(
            @RequestParam(value="siteGroupId", required=true) Long siteGroupId,
            RedirectAttributes redirectAttrs
    ) {
        try {
            indexingService.indexAllSites(siteGroupId);
            redirectAttrs.addFlashAttribute("response",
                    "Индексация всех сайтов началась");
        } catch (Exception e) {
            handleError(redirectAttrs, e);
        }
        return "redirect:/";
    }
}
