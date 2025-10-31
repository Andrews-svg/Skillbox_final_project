package com.example.searchengine.models;

import lombok.Getter;
import java.util.Arrays;



@Getter
public enum Role {
    USER("USER"),
    ADMIN("ADMIN");

    Role(String authority) {
        this.authority = authority;
    }

    private final String authority;

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
