package com.example.searchengine.controllers.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/fragments/login-fragment.html")
    public String loginFragment() {
        return "fragments/login-fragment";
    }

    @GetMapping("/fragments/registration-fragment.html")
    public String registrationFragment() {
        return "fragments/registration-fragment";
    }

    @GetMapping("/fragments/dashboard-fragment.html")
    public String dashboardFragment() {
        return "fragments/dashboard-fragment";
    }

    @GetMapping("/fragments/management-fragment.html")
    public String managementFragment() {
        return "fragments/management-fragment";
    }

    @GetMapping("/fragments/search-fragment.html")
    public String searchFragment() {
        return "fragments/search-fragment";
    }
}
