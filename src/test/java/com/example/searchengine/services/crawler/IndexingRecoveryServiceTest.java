package com.example.searchengine.services.crawler;

import com.example.searchengine.models.Status;
import com.example.searchengine.repositories.SiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndexingRecoveryService - reconcileStaleIndexing() contract")
class IndexingRecoveryServiceTest {

    @Mock private SiteRepository siteRepository;

    private IndexingRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new IndexingRecoveryService(siteRepository);
    }

    @Test
    @DisplayName("stale INDEXING rows found -> repository called with FAILED/INDEXING and fixed message")
    void staleFound_callsRepositoryWithCorrectArguments() {
        when(siteRepository.markStaleIndexingAsFailed(
                eq(Status.FAILED), eq(Status.INDEXING), any(LocalDateTime.class), anyString()))
                .thenReturn(5);

        assertThatCode(() -> service.reconcileStaleIndexing()).doesNotThrowAnyException();

        ArgumentCaptor<LocalDateTime> timeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(siteRepository).markStaleIndexingAsFailed(
                eq(Status.FAILED),
                eq(Status.INDEXING),
                timeCaptor.capture(),
                eq("Индексация прервана рестартом приложения"));
        assertThat(timeCaptor.getValue()).isNotNull();
    }

    @Test
    @DisplayName("no stale rows -> repository still called, no exception")
    void noStaleRows_stillCallsRepository() {
        when(siteRepository.markStaleIndexingAsFailed(
                eq(Status.FAILED), eq(Status.INDEXING), any(LocalDateTime.class), anyString()))
                .thenReturn(0);

        assertThatCode(() -> service.reconcileStaleIndexing()).doesNotThrowAnyException();

        verify(siteRepository).markStaleIndexingAsFailed(
                eq(Status.FAILED), eq(Status.INDEXING), any(LocalDateTime.class), anyString());
    }

    @Test
    @DisplayName("repository throws -> exception is swallowed (app startup must not fail)")
    void repositoryThrows_exceptionSwallowed() {
        when(siteRepository.markStaleIndexingAsFailed(
                eq(Status.FAILED), eq(Status.INDEXING), any(LocalDateTime.class), anyString()))
                .thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.reconcileStaleIndexing()).doesNotThrowAnyException();

        verify(siteRepository).markStaleIndexingAsFailed(
                eq(Status.FAILED), eq(Status.INDEXING), any(LocalDateTime.class), anyString());
    }
}