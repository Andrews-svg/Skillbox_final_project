package com.example.searchengine.models;

public final class ActivityActions {

    private ActivityActions() {}

    public static final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
    public static final String LOGIN_FAILED = "LOGIN_FAILED";
    public static final String LOGOUT = "LOGOUT";
    public static final String TOKEN_REFRESH = "TOKEN_REFRESH";


    public static final String REGISTRATION = "REGISTRATION";
    public static final String REGISTRATION_FAILED = "REGISTRATION_FAILED";
    public static final String ACCOUNT_ACTIVATION = "ACCOUNT_ACTIVATION";
    public static final String ACCOUNT_ACTIVATION_FAILED = "ACCOUNT_ACTIVATION_FAILED";


    public static final String PROFILE_UPDATE = "PROFILE_UPDATE";
    public static final String PASSWORD_CHANGE = "PASSWORD_CHANGE";
    public static final String PASSWORD_RESET_REQUEST = "PASSWORD_RESET_REQUEST";
    public static final String PASSWORD_RESET = "PASSWORD_RESET";


    public static final String SEARCH = "SEARCH";
    public static final String SEARCH_ADVANCED = "SEARCH_ADVANCED";
    public static final String SEARCH_FAILED = "SEARCH_FAILED";


    public static final String INDEXING_START = "INDEXING_START";
    public static final String INDEXING_COMPLETE = "INDEXING_COMPLETE";
    public static final String INDEXING_FAILED = "INDEXING_FAILED";
    public static final String INDEXING_SINGLE_SITE = "INDEXING_SINGLE_SITE";


    public static final String DETAIL_INVALID_PASSWORD = "INVALID_PASSWORD";
    public static final String DETAIL_ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String DETAIL_ACCOUNT_UNCONFIRMED = "ACCOUNT_UNCONFIRMED";
    public static final String DETAIL_ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String DETAIL_INVALID_TOKEN = "INVALID_TOKEN";
    public static final String DETAIL_USER_NOT_FOUND = "USER_NOT_FOUND";
}