package com.example.searchengine.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/admin")
public class AdminWebController {

    @GetMapping
    public String adminDashboard() {
        return "index";
    }

    @GetMapping("/modes")
    public String adminModes() {
        return "index";
    }
}