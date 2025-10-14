package com.example.searchengine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JsonTabsConfig {

    private TabsConfig tabsConfig;

    @Value("classpath:tabs.json")
    private Resource resource;

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        tabsConfig = objectMapper.readValue(resource.getInputStream(), TabsConfig.class);
    }


    public TabsConfig getTabsConfig() {
        return tabsConfig;
    }

    public void setTabsConfig(TabsConfig tabsConfig) {
        this.tabsConfig = tabsConfig;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

}