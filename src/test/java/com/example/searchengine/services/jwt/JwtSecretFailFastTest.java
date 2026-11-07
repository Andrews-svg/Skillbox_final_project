package com.example.searchengine.services.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JWT secret fail-fast")
class JwtSecretFailFastTest {

    private static final String VALID_SECRET =
            "dGVzdFNlY3JldEtleUZvckpXVFRva2VuR2VuZXJhdGlvblRoYXRJc0F0TGVhc3QzMkNoYXJzTG9uZw==";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(JwtTokenProvider.class);

    @Nested
    @DisplayName("Профиль prod: JWT_SECRET обязателен")
    class ProdProfile {

        @Test
        @DisplayName("без JWT_SECRET — контекст падает на старте")
        void withoutSecret() {
            runner.withPropertyValues("spring.profiles.active=prod")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasStackTraceContaining("JWT_SECRET");
                    });
        }

        @Test
        @DisplayName("с JWT_SECRET — контекст стартует, бин создан")
        void withSecret() {
            runner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "JWT_SECRET=" + VALID_SECRET)
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(JwtTokenProvider.class);
                    });
        }
    }

    @Nested
    @DisplayName("Профиль по умолчанию: работает на дефолтном ключе")
    class DefaultProfile {

        @Test
        @DisplayName("без переменной окружения — контекст стартует")
        void withoutEnv() {
            runner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(JwtTokenProvider.class);
            });
        }
    }
}