package com.example.searchengine.services.indexing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("IndexingState")
class IndexingStateTest {

    private IndexingState state;

    @BeforeEach
    void setUp() {
        state = new IndexingState();
    }

    @Nested
    @DisplayName("Флаг индексации (isActive/setActive)")
    class ActiveFlag {

        @Test
        @DisplayName("по умолчанию индексация не активна")
        void defaultIsNotActive() {
            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("setActive(true) включает флаг")
        void setActiveTrueActivates() {
            state.setActive(true);

            assertThat(state.isActive()).isTrue();
        }

        @Test
        @DisplayName("setActive(false) выключает флаг")
        void setActiveFalseDeactivates() {
            state.setActive(true);
            state.setActive(false);

            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("повторный setActive идемпотентен")
        void setActiveIsIdempotent() {
            state.setActive(true);
            state.setActive(true);
            assertThat(state.isActive()).isTrue();

            state.setActive(false);
            state.setActive(false);
            assertThat(state.isActive()).isFalse();
        }
    }

    @Nested
    @DisplayName("Счётчики сессии")
    class SessionCounters {

        @Test
        @DisplayName("startSession обнуляет счётчики и фиксирует время старта")
        void startSessionResetsCounters() {
            state.addSessionSite(1L);
            state.incrementPages(1L, 5);
            state.incrementLemmas(1L, 10);
            state.incrementErrors(1L, 2);

            state.startSession();

            assertThat(state.getSessionSiteIds()).isEmpty();
            assertThat(state.getTotalPages()).isZero();
            assertThat(state.getTotalLemmas()).isZero();
            assertThat(state.getTotalErrors()).isZero();
            assertThat(state.getSessionStartTime()).isPositive();
        }

        @Test
        @DisplayName("incrementPages накапливает по сайту и суммирует в total")
        void incrementPagesAccumulates() {
            state.incrementPages(1L, 3);
            state.incrementPages(1L, 2);
            state.incrementPages(2L, 7);

            assertThat(state.getSessionPages(1L)).isEqualTo(5);
            assertThat(state.getSessionPages(2L)).isEqualTo(7);
            assertThat(state.getTotalPages()).isEqualTo(12);
        }

        @Test
        @DisplayName("getSessionPages для неизвестного сайта возвращает 0")
        void unknownSiteReturnsZero() {
            assertThat(state.getSessionPages(999L)).isZero();
            assertThat(state.getSessionLemmas(999L)).isZero();
            assertThat(state.getSessionErrors(999L)).isZero();
        }
    }
}

