package com.example.searchengine.controllers.api;

import com.example.searchengine.services.SiteService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/page")
public class PageController {

    @Autowired
    private SiteService siteService;

    private static final Logger logger =
            LoggerFactory.getLogger(PageController.class);



    @GetMapping("/group/{groupId}/ids")
    public ResponseEntity<Map<String, Object>> getSiteIdsByGroup(
            @PathVariable Long groupId) {
        List<Long> siteIds = siteService.getSiteIdsByGroupId(groupId);
        Map<String, Object> result = new HashMap<>();
        result.put("siteIds", siteIds);
        return ResponseEntity.ok(result);
    }
}
