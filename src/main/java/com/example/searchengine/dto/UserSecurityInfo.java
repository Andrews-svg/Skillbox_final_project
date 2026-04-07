package com.example.searchengine.dto;

import com.example.searchengine.models.AccountStatus;

import java.util.List;

public class UserSecurityInfo {
    private String username;
    private Long id;
    private String email;
    private List<String> roles;
    private boolean enabled;
    private boolean accountNonLocked;
    private AccountStatus accountStatus;


    public UserSecurityInfo(String username, Long id, String email,
                            List<String> roles, boolean enabled,
                            boolean accountNonLocked, AccountStatus accountStatus) {
        this.username = username;
        this.id = id;
        this.email = email;
        this.roles = roles;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.accountStatus = accountStatus;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        this.accountNonLocked = accountNonLocked;
    }

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }
}
