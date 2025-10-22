package com.example.searchengine.models;

import jakarta.validation.constraints.NotBlank;



public class LoginForm {

    @NotBlank(message = "{login.username.not_blank}")
    private String username;

    @NotBlank(message = "{login.password.not_blank}")
    private String password;


    public LoginForm() {}

    public LoginForm(String username, String password) {
        this.username = username;
        this.password = password;
    }


    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "LoginForm{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                '}';
    }
}
