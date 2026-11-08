package com.example.searchengine.exceptions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("GlobalExceptionHandler — контракт исключений -> HTTP")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;
    private LocalValidatorFactoryBean validator;

    @BeforeEach
    void setUp() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void tearDown() {
        GlobalExceptionHandler.finishManualRestart();
        if (validator != null) {
            validator.close();
        }
    }

    @Test
    @DisplayName("CustomNotFoundException -> 404, message goes to $.error")
    void notFound_returns404WithMessage() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.result").value(false))
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.error").value("Сущность не найдена"));
    }

    @Test
    @DisplayName("IndexingStartFailureException -> 500, message goes to $.error")
    void indexingStartFailure_returns500WithMessage() throws Exception {
        mockMvc.perform(get("/test/indexing-failure"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.error").value("Индексация уже запущена"));
    }

    @Test
    @DisplayName("IllegalArgumentException -> 400, message goes to $.error")
    void illegalArgument_returns400WithMessage() throws Exception {
        mockMvc.perform(get("/test/illegal-arg"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error").value("Некорректный аргумент"));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException (1 error) -> 400, format 'field: msg'")
    void validation_singleFieldError_returns400WithFieldMessage() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"x@y.z\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error").value(allOf(
                        containsString("name:"),
                        containsString("must not be blank")
                )));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException (2 errors) -> 400, joined by '; '")
    void validation_twoFieldErrors_joinedBySemicolonSpace() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.error").value(allOf(
                        containsString("name:"),
                        containsString("email:"),
                        containsString("; ")
                )));
    }

    @Test
    @DisplayName("ClientAbortException (normal) -> 503, fixed message")
    void clientAbort_returns503WithFixedMessage() throws Exception {
        mockMvc.perform(get("/test/client-abort"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value(503))
                .andExpect(jsonPath("$.error").value("Сервис временно недоступен."));
    }

    @Test
    @DisplayName("ClientAbortException (manual restart) -> 503, empty body")
    void clientAbort_duringManualRestart_returns503WithEmptyBody() throws Exception {
        GlobalExceptionHandler.startManualRestart();

        mockMvc.perform(get("/test/client-abort"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("Exception (generic) -> 500, NO leakage of ex.getMessage()")
    void genericException_returns500WithoutLeakingMessage() throws Exception {
        mockMvc.perform(get("/test/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.error").value(
                        "Произошла внутренняя ошибка сервера. Обратитесь в поддержку."))
                .andExpect(content().string(not(containsString("secret leak"))));
    }

    // ===== test-only controller =====

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new CustomNotFoundException("Сущность не найдена");
        }

        @GetMapping("/test/indexing-failure")
        public void indexingFailure() {
            throw new IndexingStartFailureException("Индексация уже запущена");
        }

        @GetMapping("/test/illegal-arg")
        public void illegalArg() {
            throw new IllegalArgumentException("Некорректный аргумент");
        }

        @GetMapping("/test/generic")
        public void generic() {
            throw new RuntimeException("secret leak — не должно попасть в ответ");
        }

        @GetMapping("/test/client-abort")
        public void clientAbort() throws ClientAbortException {
            throw new ClientAbortException("client aborted");
        }

        @PostMapping("/test/validation")
        public void validation(@Valid @RequestBody TestDto dto) {
            // тело не важно, важна валидация
        }
    }

    static class TestDto {
        @NotBlank(message = "must not be blank")
        private String name;

        @NotBlank(message = "must not be blank")
        private String email;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}