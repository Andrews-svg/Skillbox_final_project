package com.example.searchengine.controllers.auth;

import com.example.searchengine.exceptions.UserRegistrationException;
import com.example.searchengine.services.EmailService;
import com.example.searchengine.services.UserActivityService;
import com.example.searchengine.services.jwt.JwtTokenService;
import com.example.searchengine.services.user.UserQueryServiceImpl;
import com.example.searchengine.services.user.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import com.example.searchengine.services.VisitCounterService;
import com.example.searchengine.validation.PasswordValidator;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController — /api/auth/register: обработка исключений")
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private UserServiceImpl userServiceImpl;
    @MockBean private UserQueryServiceImpl userQueryServiceImpl;
    @MockBean private AuthenticationManager authenticationManager;
    @MockBean private JwtTokenService jwtTokenService;
    @MockBean private EmailService emailService;
    @MockBean private UserActivityService userActivityService;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private VisitCounterService visitCounterService;
    @MockBean private PasswordValidator passwordValidator;

    private static final Map<String, String> VALID_BODY = Map.of(
            "username", "testuser",
            "lastName", "Test",
            "email", "test@example.com",
            "password", "Password1!",
            "confirmPassword", "Password1!"
    );

    @Nested
    @DisplayName("Исключения из registerNewUser")
    class RegisterExceptionHandling {

        @Test
        @DisplayName("IllegalArgumentException → 400 через GlobalExceptionHandler, аудит сохранён")
        void illegalArgumentReturnsBadRequest() throws Exception {
            stubPreChecksPass();
            doThrow(new IllegalArgumentException("Email не может быть пустым"))
                    .when(userServiceImpl).registerNewUser(any());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_BODY)))
                    .andExpect(status().isBadRequest());

            verify(userActivityService).logRegistrationFailed(any(), any(), any(), any());
        }

        @Test
        @DisplayName("UserRegistrationException → 500 через catch (Exception) в контроллере")
        void userRegistrationExceptionReturnsInternalError() throws Exception {
            stubPreChecksPass();
            doThrow(new UserRegistrationException("Ошибка сохранения"))
                    .when(userServiceImpl).registerNewUser(any());

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(VALID_BODY)))
                    .andExpect(status().isInternalServerError());

            verify(userActivityService).logRegistrationFailed(any(), any(), any(), any());
        }

        private void stubPreChecksPass() {
            when(userQueryServiceImpl.existsByUsername(anyString())).thenReturn(false);
            when(userQueryServiceImpl.existsByEmail(anyString())).thenReturn(false);
            when(passwordValidator.isValid(anyString())).thenReturn(true);
        }
    }
}
