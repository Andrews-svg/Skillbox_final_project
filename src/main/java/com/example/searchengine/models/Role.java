package com.example.searchengine.models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;


@Getter
@RequiredArgsConstructor
@AllArgsConstructor
public enum Role {
    USER("USER"),
    ADMIN("ADMIN");

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
