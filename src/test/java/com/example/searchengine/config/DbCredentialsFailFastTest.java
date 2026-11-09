package com.example.searchengine.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DB credentials fail-fast (integration)")
class DbCredentialsFailFastTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(DbCredentialsValidator.class);

    @Nested
    @DisplayName("prod profile: DB_USER обязателен и не root")
    class ProdProfile {

        @Test
        @DisplayName("DB_USER пустой — контекст падает на placeholder (слой 1)")
        void withoutDbUser() {
            runner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "DB_USER=")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasStackTraceContaining("DB_USER");
                    });
        }

        @Test
        @DisplayName("DB_USER=root — контекст падает на валидаторе (слой 2)")
        void withRootUser() {
            runner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "DB_USER=root")
                    .run(context -> {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure())
                                .hasStackTraceContaining("DB_USER=root is forbidden");
                    });
        }

        @Test
        @DisplayName("DB_USER=appuser — контекст стартует, бин создан")
        void withAppUser() {
            runner.withPropertyValues(
                            "spring.profiles.active=prod",
                            "DB_USER=appuser")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(DbCredentialsValidator.class);
                    });
        }
    }

    @Nested
    @DisplayName("dev / default profile: root допустим")
    class DevProfile {

        @Test
        @DisplayName("dev без DB_USER (root по default) — стартует")
        void devWithDefaultRoot() {
            runner.withPropertyValues("spring.profiles.active=dev")
                    .run(context -> {
                        assertThat(context).hasNotFailed();
                        assertThat(context).hasSingleBean(DbCredentialsValidator.class);
                    });
        }

        @Test
        @DisplayName("без профиля (root по default) — стартует")
        void noProfileDefaultRoot() {
            runner.run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(DbCredentialsValidator.class);
            });
        }
    }
}
