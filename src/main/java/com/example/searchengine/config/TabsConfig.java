package com.example.searchengine.config;

import lombok.Data;


    @Data
    public class TabsConfig {

        private String dashboard;
        private String management;
        private String search;


        public String getDashboard() {
            return dashboard;
        }

        public void setDashboard(String dashboard) {
            this.dashboard = dashboard;
        }

        public String getManagement() {
            return management;
        }

        public void setManagement(String management) {
            this.management = management;
        }

        public String getSearch() {
            return search;
        }

        public void setSearch(String search) {
            this.search = search;
        }

    }

