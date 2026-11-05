package com.example.searchengine.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PasswordValidator")
class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
    }

    @Nested
    @DisplayName("Длина пароля (8–20)")
    class Length {

        @Test
        @DisplayName("null отклоняется")
        void nullRejected() {
            assertThat(validator.isValid(null)).isFalse();
        }

        @Test
        @DisplayName("пустая строка отклоняется")
        void emptyRejected() {
            assertThat(validator.isValid("")).isFalse();
        }

        @Test
        @DisplayName("7 символов отклоняется (ниже минимума)")
        void tooShortRejected() {
            assertThat(validator.isValid("Aa1!aaa")).isFalse();
        }

        @Test
        @DisplayName("21 символ отклоняется (выше максимума)")
        void tooLongRejected() {
            assertThat(validator.isValid("Aa1!aaaaaaaaaaaaaaaaa")).isFalse();
        }
    }

    @Nested
    @DisplayName("Обязательные классы символов")
    class RequiredCharacters {

        @Test
        @DisplayName("без цифры отклоняется")
        void withoutDigitRejected() {
            assertThat(validator.isValid("Password!")).isFalse();
        }

        @Test
        @DisplayName("без строчной буквы отклоняется")
        void withoutLowercaseRejected() {
            assertThat(validator.isValid("PASSWORD1!")).isFalse();
        }

        @Test
        @DisplayName("без заглавной буквы отклоняется")
        void withoutUppercaseRejected() {
            assertThat(validator.isValid("password1!")).isFalse();
        }

        @Test
        @DisplayName("без спецсимвола отклоняется")
        void withoutSpecialCharRejected() {
            assertThat(validator.isValid("Password1")).isFalse();
        }

        @Test
        @DisplayName("дефис не считается спецсимволом")
        void hyphenIsNotSpecialChar() {
            assertThat(validator.isValid("Password-1")).isFalse();
        }
    }

    @Nested
    @DisplayName("Запрещённое содержимое")
    class ForbiddenContent {

        @Test
        @DisplayName("пробел отклоняется")
        void spaceRejected() {
            assertThat(validator.isValid("Password 1!")).isFalse();
        }

        @Test
        @DisplayName("последовательность 123456 отклоняется")
        void sequence123456Rejected() {
            assertThat(validator.isValid("Abc123456!")).isFalse();
        }

        @Test
        @DisplayName("последовательность abcdef отклоняется")
        void sequenceAbcdefRejected() {
            assertThat(validator.isValid("Xxabcdef1!")).isFalse();
        }

        @Test
        @DisplayName("последовательность qwerty отклоняется")
        void sequenceQwertyRejected() {
            assertThat(validator.isValid("xxqwerty1!")).isFalse();
        }
    }

    @Nested
    @DisplayName("Валидные пароли")
    class ValidPasswords {

        @Test
        @DisplayName("канонический валидный пароль принимается")
        void canonicalValid() {
            assertThat(validator.isValid("Password1!")).isTrue();
        }

        @Test
        @DisplayName("ровно 8 символов (нижняя граница) принимается")
        void minLengthBoundary() {
            assertThat(validator.isValid("Aa1!aaaa")).isTrue();
        }

        @Test
        @DisplayName("ровно 20 символов (верхняя граница) принимается")
        void maxLengthBoundary() {
            assertThat(validator.isValid("Aa1!aaaaaaaaaaaaaaaa")).isTrue();
        }
    }
}
