package com.example.searchengine.dto.registration;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegistrationDTO {

    @NotNull(message = "Имя пользователя обязательно.")
    @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов.")
    private String username;

    @NotNull(message = "Фамилия обязательна.")
    @Size(max = 50, message = "Фамилия не может превышать 50 символов.")
    private String lastName;

    @NotNull(message = "E-mail обязателен.")
    @Email(message = "Неверный формат E-mail.")
    private String email;

    @NotNull(message = "Пароль обязателен.")
    @Size(min = 8, max = 20, message = "Минимальная длина пароля - 8 символов.")
    private String password;

    @NotNull(message = "Подтверждение пароля обязательно.")
    @Size(min = 8, max = 20, message = "Минимальная длина подтверждения пароля - 8 символов.")
    private String confirmPassword;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    @Override
    public String toString() {
        return "RegistrationDTO{" +
                "username='" + username + '\'' +
                ", lastName='" + lastName + '\'' +
                ", email='" + email + '\'' +
                ", password='******', " +
                ", confirmPassword='******'}";
    }
}