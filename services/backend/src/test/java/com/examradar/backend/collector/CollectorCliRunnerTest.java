package com.examradar.backend.collector;

import com.examradar.backend.domain.Source;
import com.examradar.backend.notification.FcmPushService;
import com.examradar.backend.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectorCliRunnerTest {

    @Mock
    private IngestionEngine ingestionEngine;

    @Mock
    private SourceRepository sourceRepository;

    @Mock
    private FcmPushService fcmPushService;

    private CollectorCliRunner runner;

    @BeforeEach
    void setUp() {
        runner = new CollectorCliRunner(ingestionEngine, sourceRepository, fcmPushService);
    }

    @Test
    @DisplayName("Should ingest all enabled sources and dispatch push queue when --all is supplied")
    void shouldIngestAllSourcesWhenAllFlagSupplied() {
        Source s1 = new Source("src-upsc", "UPSC", "UPSC", "https://upsc.gov.in", null, null, 60, "1.0", null);
        Source s2 = new Source("src-ssc", "SSC", "SSC", "https://ssc.gov.in", null, null, 60, "1.0", null);
        when(sourceRepository.findByIsEnabledTrue()).thenReturn(List.of(s1, s2));

        runner.run("--all");

        verify(ingestionEngine).runIngestionForSource("src-upsc");
        verify(ingestionEngine).runIngestionForSource("src-ssc");
        verify(fcmPushService).processDueNotificationJobs();
    }

    @Test
    @DisplayName("Should ingest only targeted source when --source flag is supplied")
    void shouldIngestTargetSourceWhenSourceFlagSupplied() {
        runner.run("--source=src-ibps");

        verify(sourceRepository, never()).findByIsEnabledTrue();
        verify(ingestionEngine).runIngestionForSource("src-ibps");
        verify(fcmPushService).processDueNotificationJobs();
    }

    @Test
    @DisplayName("Should continue running remaining sources when one source throws an exception")
    void shouldContinueWhenOneSourceFails() {
        Source s1 = new Source("src-upsc", "UPSC", "UPSC", "https://upsc.gov.in", null, null, 60, "1.0", null);
        Source s2 = new Source("src-ssc", "SSC", "SSC", "https://ssc.gov.in", null, null, 60, "1.0", null);
        when(sourceRepository.findByIsEnabledTrue()).thenReturn(List.of(s1, s2));
        doThrow(new RuntimeException("Simulated connection timeout")).when(ingestionEngine).runIngestionForSource("src-upsc");

        runner.run("--all");

        verify(ingestionEngine).runIngestionForSource("src-upsc");
        verify(ingestionEngine).runIngestionForSource("src-ssc");
        verify(fcmPushService).processDueNotificationJobs();
    }

    @Test
    @DisplayName("Should do nothing when no collector CLI arguments are supplied")
    void shouldStandbyWhenNoArgsSupplied() {
        runner.run();

        verify(sourceRepository, never()).findByIsEnabledTrue();
        verify(ingestionEngine, never()).runIngestionForSource(anyString());
        verify(fcmPushService, never()).processDueNotificationJobs();
    }
}
