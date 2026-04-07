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
}
