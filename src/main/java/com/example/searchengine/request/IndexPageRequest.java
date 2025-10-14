package com.example.searchengine.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

public class IndexPageRequest {

    @NotEmpty(message = "URL не должен быть пустым")
    @Pattern(regexp = "^(http://|https://).+", message = "Некорректный URL")
    private String url;

    @NotEmpty(message = "Имя не должно быть пустым")
    private String name;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "IndexPageRequest{" +
                "url='" + url + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}