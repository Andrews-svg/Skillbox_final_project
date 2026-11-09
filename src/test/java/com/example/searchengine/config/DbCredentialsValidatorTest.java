package com.example.searchengine.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DbCredentialsValidator - fail-fast contract")
class DbCredentialsValidatorTest {

    private static MockEnvironment env(String... profiles) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profiles);
        return environment;
    }

    @Nested
    @DisplayName("prod profile")
    class ProdProfile {

        @Test
        @DisplayName("DB_USER=root -> IllegalStateException")
        void rootForbidden() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env("prod"), "root");

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_USER");
        }

        @Test
        @DisplayName("empty DB_USER -> IllegalStateException")
        void emptyForbidden() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env("prod"), "");

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_USER");
        }

        @Test
        @DisplayName("unresolved ${DB_USER} placeholder -> IllegalStateException")
        void unresolvedPlaceholder() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env("prod"), "${DB_USER}");

            assertThatThrownBy(validator::validate)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DB_USER");
        }

        @Test
        @DisplayName("DB_USER=appuser -> ok")
        void nonRootOk() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env("prod"), "appuser");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("profiles dev,prod (prod active) -> still checks")
        void multipleProfilesWithProd() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env("dev", "prod"), "root");

            assertThatThrownBy(validator::validate).isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("default profile")
    class DefaultProfile {

        @Test
        @DisplayName("DB_USER=root in dev -> allowed")
        void rootAllowedInDev() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env("dev"), "root");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("no active profile -> root allowed")
        void rootAllowedWithoutProfile() {
            DbCredentialsValidator validator = new DbCredentialsValidator(env(), "root");

            assertThatCode(validator::validate).doesNotThrowAnyException();
        }
    }
}
