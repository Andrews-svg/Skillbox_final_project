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
    @DisplayName("Флаг индексации (ref-count + stop)")
    class ActiveFlag {

        @Test
        @DisplayName("по умолчанию индексация не активна")
        void defaultIsNotActive() {
            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("startIndexing включает, endIndexing выключает")
        void startThenEnd_activatesAndDeactivates() {
            state.startIndexing();
            assertThat(state.isActive()).isTrue();

            state.endIndexing();
            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("два startIndexing + один endIndexing — всё ещё активна (ref-count)")
        void refCountTwoStartsOneEnd_stillActive() {
            state.startIndexing();
            state.startIndexing();
            state.endIndexing();
            assertThat(state.isActive()).isTrue();

            state.endIndexing();
            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("endIndexing при 0 не уходит в минус, следующий startIndexing работает")
        void endIndexingAtZero_doesNotUnderflow() {
            state.endIndexing();
            state.endIndexing();
            assertThat(state.isActive()).isFalse();

            state.startIndexing();
            assertThat(state.isActive()).isTrue();
        }

        @Test
        @DisplayName("requestStop форсирует isActive=false даже при активном счётчике")
        void requestStop_forcesInactive() {
            state.startIndexing();
            state.requestStop();
            assertThat(state.isActive()).isFalse();
        }

        @Test
        @DisplayName("reset обнуляет счётчик и снимает stopRequested")
        void reset_clearsAll() {
            state.startIndexing();
            state.startIndexing();
            state.requestStop();
            assertThat(state.isActive()).isFalse();

            state.reset();
            assertThat(state.isActive()).isFalse();

            state.startIndexing();
            assertThat(state.isActive()).isTrue();
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

