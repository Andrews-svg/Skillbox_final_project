package com.example.searchengine.controllers.web;

import com.example.searchengine.config.JsonTabsConfig;
import com.example.searchengine.config.TabsConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class LayoutController {

    private final TabsConfig tabsConfig;

    public LayoutController(JsonTabsConfig jsonTabsConfig) {
        this.tabsConfig = jsonTabsConfig.getTabsConfig();
    }

    @RequestMapping("/")
    public String home(Model model) {
        model.addAttribute("DASHBOARD_TAB_NAME", tabsConfig.getDashboard());
        model.addAttribute("MANAGEMENT_TAB_NAME", tabsConfig.getManagement());
        model.addAttribute("SEARCH_TAB_NAME", tabsConfig.getSearch());
        return "index";
    }
}
