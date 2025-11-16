package com.example.searchengine.statisticsResponse;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
class LemmaRequest {
    private String input;
    private String siteURL;


    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }

    public String getSiteURL() { return siteURL; }
    public void setSiteURL(String siteURL) { this.siteURL = siteURL; }
}
