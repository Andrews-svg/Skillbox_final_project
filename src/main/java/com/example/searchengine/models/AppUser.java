package com.example.searchengine.models;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "app_users", indexes = {
        @Index(name = "idx_username", columnList = "username"),
        @Index(name = "idx_email", columnList = "email"),
        @Index(name = "idx_activation_token", columnList = "activationToken"),
        @Index(name = "idx_status", columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
public class AppUser implements UserDetails {

    private static final Logger log = LoggerFactory.getLogger(AppUser.class);
    private static final int PASSWORD_HASH_LENGTH = 60;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя пользователя обязательно")
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @NotBlank(message = "Фамилия обязательна")
    @Size(max = 50, message = "Фамилия не может превышать 50 символов")
    @Column(nullable = false, length = 50)
    private String lastName;

    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    @Column(name = "email", nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Пароль обязателен")
    @Column(nullable = false, length = 60)
    private String password;

    @Column(unique = true, length = 36)
    private String activationToken;

    @Column(name = "reset_token", length = 36)
    private String resetToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.UNCONFIRMED;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "account_expired_at")
    private Instant accountExpiredAt;

    @Column(name = "credentials_expired_at")
    private Instant credentialsExpiredAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "failed_attempts")
    private Integer failedAttempts = 0;


    public AppUser() {
    }

    public AppUser(String username, String lastName, String email, String password) {
        this.username = username;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getActivationToken() {
        return activationToken;
    }

    public void setActivationToken(String activationToken) {
        this.activationToken = activationToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public Instant getAccountExpiredAt() {
        return accountExpiredAt;
    }

    public void setAccountExpiredAt(Instant accountExpiredAt) {
        this.accountExpiredAt = accountExpiredAt;
    }

    public Instant getCredentialsExpiredAt() {
        return credentialsExpiredAt;
    }

    public void setCredentialsExpiredAt(Instant credentialsExpiredAt) {
        this.credentialsExpiredAt = credentialsExpiredAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Instant lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public Integer getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(Integer failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    // Business methods
    public void incrementFailedAttempts() {
        this.failedAttempts = (this.failedAttempts == null ? 1 : this.failedAttempts + 1);
    }

    public void resetFailedAttempts() {
        this.failedAttempts = 0;
    }

    public void lockAccount(Instant until) {
        this.lockedUntil = until;
    }

    public void unlockAccount() {
        this.lockedUntil = null;
    }

    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }

    public boolean isPasswordAlreadyEncoded() {
        return password != null &&
                password.length() == PASSWORD_HASH_LENGTH &&
                (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles == null || roles.isEmpty()) {
            log.debug("User {} has no roles assigned", username);
            return List.of();
        }

        return roles.stream()
                .map(Role::getAuthority)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() {
        if (accountExpiredAt == null) {
            return true;
        }
        boolean isNonExpired = !Instant.now().isAfter(accountExpiredAt);
        if (!isNonExpired) {
            log.debug("Account {} has expired at {}", username, accountExpiredAt);
        }
        return isNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil == null) {
            return true;
        }
        boolean isNonLocked = !Instant.now().isBefore(lockedUntil);
        if (!isNonLocked) {
            log.debug("Account {} is locked until {}", username, lockedUntil);
        }
        return isNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (credentialsExpiredAt == null) {
            return true;
        }
        boolean isNonExpired = !Instant.now().isAfter(credentialsExpiredAt);
        if (!isNonExpired) {
            log.debug("Credentials for user {} expired at {}", username, credentialsExpiredAt);
        }
        return isNonExpired;
    }

    @Override
    public boolean isEnabled() {
        boolean isEnabled = AccountStatus.CONFIRMED.equals(status);
        if (!isEnabled) {
            log.debug("Account {} is not enabled, status: {}", username, status);
        }
        return isEnabled;
    }


    public String getFullName() {
        return username + " " + lastName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUser appUser = (AppUser) o;
        return id != null && id.equals(appUser.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "AppUser{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", status=" + status +
                ", roles=" + (roles != null ? roles.stream().map(Role::getAuthority).toList() : "[]") +
                ", createdAt=" + createdAt +
                ", lastLoginAt=" + lastLoginAt +
                '}';
    }
}