package com.example.searchengine.dto.statistics.requests;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public class IndexPageRequest {

    @NotEmpty(message = "URL не должен быть пустым")
    @URL(message = "Некорректный URL")
    private String url;

    @NotEmpty(message = "Название не должно быть пустым")
    @Size(max = 100, message = "Название не должно превышать 100 символов")
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