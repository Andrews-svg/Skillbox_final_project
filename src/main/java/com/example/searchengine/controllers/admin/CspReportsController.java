package com.example.searchengine.controllers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CspReportsController {


        private static final Logger LOGGER = LoggerFactory.getLogger(CspReportsController.class);

        @PostMapping("/csp-reports")
        public void handleCspReport(@RequestBody String reportJson) {
            LOGGER.info(reportJson);
        }
    }