package com.example.searchengine.models;

import java.util.Arrays;

public enum Role {
    USER("USER"),
    ADMIN("ADMIN");

    private final String authority;

    Role(final String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }


    public static Role fromString(final String authority) {
        return Arrays.stream(values())
                .filter(r -> r.getAuthority().equals(authority))
                .findFirst()
                .orElse(null);
    }

    @Override
    public String toString() {
        return authority;
    }
}
