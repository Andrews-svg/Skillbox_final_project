package com.example.searchengine.models;

public enum Role {
    USER("USER"),
    ADMIN("ADMIN");

    private final String authority;


    Role(String authority) {
        this.authority = authority;
    }


    public String getAuthority() {
        return authority;
    }


    public static Role fromString(final String authority) {
        if (authority != null && !authority.isEmpty()) {
            for (Role role : values()) {
                if (role.getAuthority().equals(authority)) {
                    return role;
                }
            }
        }
        return null;
    }


    @Override
    public String toString() {
        return authority;
    }
}