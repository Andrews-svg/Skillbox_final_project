package com.example.searchengine.services;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


@Service
public class HealthCheckService {


    public List<Boolean> areSitesAvailable(List<String> sitesToCheck) {
        List<Boolean> availabilityList = new ArrayList<>();
        for (String site : sitesToCheck) {
            try {
                URL url = new URL(site);
                HttpURLConnection connection =
                        (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(2000);
                connection.setReadTimeout(2000);
                int responseCode = connection.getResponseCode();
                availabilityList.add(
                        responseCode >= 200 && responseCode < 400);
            } catch (IOException e) {
                availabilityList.add(false);
            }
        }
        return availabilityList;
    }
}
