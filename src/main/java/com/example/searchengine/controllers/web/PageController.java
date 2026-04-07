package com.example.searchengine.controllers.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
public class PageController {


    @GetMapping({
            "/login",
            "/register",
            "/activate",
            "/forgot-password",
            "/reset-password",
            "/dashboard",
            "/management",
            "/search"
    })
    public String spa() {
        return "index";
    }


    @GetMapping("/fragments/login-fragment")
    public String loginFragment() {
        return "fragments/login-fragment :: login-content";
    }

    @GetMapping("/fragment/registration-fragment")
    public String registrationFragment() {
        return "fragments/registration-fragment :: registration-content";
    }
}