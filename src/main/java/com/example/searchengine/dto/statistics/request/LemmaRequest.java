package com.example.searchengine.dto.statistics.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class LemmaRequest {
    @NotBlank(message = "Input cannot be empty")
    private String input;

    @Pattern(regexp = "^(http://|https://).*", message = "Site URL must start with http:// or https://")
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