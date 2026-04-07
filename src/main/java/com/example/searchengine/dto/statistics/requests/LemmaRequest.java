package com.example.searchengine.dto.statistics.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

public class LemmaRequest {
    @NotBlank(message = "Ввод недопустим")
    private String input;

    @NotNull(message = "Сайт не указан")
    @URL(message = "Некорректный URL сайта")
    private String siteURL;


    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getSiteURL() {
        return siteURL;
    }

    public void setSiteURL(String siteURL) {
        this.siteURL = siteURL;
    }
}