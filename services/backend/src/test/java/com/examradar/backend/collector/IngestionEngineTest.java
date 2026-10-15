package com.examradar.backend.collector;

import com.examradar.backend.domain.*;
import com.examradar.backend.domain.enums.*;
import com.examradar.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IngestionEngineTest {

    private final SourceAdapter mockAdapter = mock(SourceAdapter.class);
    private final SourceRepository sourceRepository = mock(SourceRepository.class);
    private final SourceFetchRunRepository fetchRunRepository = mock(SourceFetchRunRepository.class);
    private final SourceDocumentRepository documentRepository = mock(SourceDocumentRepository.class);
    private final NoticeRepository noticeRepository = mock(NoticeRepository.class);
    private final NoticeVersionRepository versionRepository = mock(NoticeVersionRepository.class);
    private final NoticeDateRepository dateRepository = mock(NoticeDateRepository.class);
    private final ExamRepository examRepository = mock(ExamRepository.class);
    private final SourceHealthSnapshotRepository healthSnapshotRepository = mock(SourceHealthSnapshotRepository.class);

    private IngestionEngine engine;
    private Source testSource;
    private Exam testExam;

    @BeforeEach
    void setUp() {
        when(mockAdapter.getSourceId()).thenReturn("src-test");

        engine = new IngestionEngine(
                List.of(mockAdapter),
                sourceRepository,
                fetchRunRepository,
                documentRepository,
                noticeRepository,
                versionRepository,
                dateRepository,
                examRepository,
                healthSnapshotRepository
        );

        testSource = new Source("src-test", "Test Org", "Test Notice Board", "https://test.gov.in",
                SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", "Notes");

        testExam = new Exam("exam-test", null, "Test Org", "Test Exam", "TE", "https://test.gov.in", "Desc");
    }

    @Test
    void testRunIngestion_newNoticeCreation() {
        when(sourceRepository.findById("src-test")).thenReturn(Optional.of(testSource));
        when(examRepository.findById("exam-test")).thenReturn(Optional.of(testExam));
        when(noticeRepository.findByOfficialNoticeUrl(anyString())).thenReturn(Optional.empty());

        ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                "exam-test", "src-test", NoticeType.RECRUITMENT, "Test Title",
                "Test Summary", "https://test.gov.in/notice-1", null,
                NoticeStatus.PUBLISHED, VerificationStatus.VERIFIED, LocalDate.now(),
                List.of(new ExtractedDateDto(DateType.APPLICATION_START, LocalDate.now(), false, false, "today", "snippet", 1.0)),
                "content", "hash123", "Eligibility", 0.95
        );

        ExtractionResult extractionResult = ExtractionResult.success(
                "src-test", true, "hash123", 200, 150L, List.of(noticeDto)
        );

        when(mockAdapter.fetchAndExtract(eq(testSource), any())).thenReturn(extractionResult);
        when(noticeRepository.save(any(Notice.class))).thenAnswer(i -> i.getArgument(0));

        IngestionEngine.IngestionSummary summary = engine.runIngestionForSource("src-test");

        assertTrue(summary.isSuccess());
        assertEquals(1, summary.newNotices());
        assertEquals(0, summary.revisedNotices());
        verify(noticeRepository, times(1)).save(any(Notice.class));
        verify(versionRepository, times(1)).save(any(NoticeVersion.class));
    }

    @Test
    void testRunIngestion_quarantineAmbiguousDateSequence() {
        when(sourceRepository.findById("src-test")).thenReturn(Optional.of(testSource));
        when(examRepository.findById("exam-test")).thenReturn(Optional.of(testExam));
        when(noticeRepository.findByOfficialNoticeUrl(anyString())).thenReturn(Optional.empty());

        // Inconsistent dates: deadline is BEFORE opening date!
        LocalDate openDate = LocalDate.now();
        LocalDate deadlineBeforeOpen = LocalDate.now().minusDays(5);

        ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                "exam-test", "src-test", NoticeType.RECRUITMENT, "Quarantined Notice",
                "Test Summary", "https://test.gov.in/notice-2", null,
                NoticeStatus.PUBLISHED, VerificationStatus.VERIFIED, LocalDate.now(),
                List.of(
                        new ExtractedDateDto(DateType.APPLICATION_START, openDate, false, false, "today", "snippet", 1.0),
                        new ExtractedDateDto(DateType.APPLICATION_DEADLINE, deadlineBeforeOpen, false, false, "past", "snippet", 1.0)
                ),
                "content", "hash456", "Eligibility", 0.95
        );

        ExtractionResult extractionResult = ExtractionResult.success(
                "src-test", true, "hash456", 200, 150L, List.of(noticeDto)
        );

        when(mockAdapter.fetchAndExtract(eq(testSource), any())).thenReturn(extractionResult);
        when(noticeRepository.save(any(Notice.class))).thenAnswer(i -> {
            Notice n = i.getArgument(0);
            assertEquals(NoticeStatus.VALIDATION_REQUIRED, n.getStatus());
            assertEquals(VerificationStatus.QUARANTINED, n.getVerificationStatus());
            return n;
        });

        IngestionEngine.IngestionSummary summary = engine.runIngestionForSource("src-test");

        assertTrue(summary.isSuccess());
        assertEquals(1, summary.newNotices());
    }

    @Test
    void testRunIngestion_unchangedHashSkipsProcessing() {
        testSource.setLastSuccessfulContentHash("same-hash");
        when(sourceRepository.findById("src-test")).thenReturn(Optional.of(testSource));

        ExtractionResult noChangeResult = ExtractionResult.noChange("src-test", "same-hash", 200, 80L);
        when(mockAdapter.fetchAndExtract(eq(testSource), eq("same-hash"))).thenReturn(noChangeResult);

        IngestionEngine.IngestionSummary summary = engine.runIngestionForSource("src-test");

        assertTrue(summary.isSuccess());
        assertEquals(0, summary.newNotices());
        assertEquals(0, summary.revisedNotices());
        verify(noticeRepository, never()).save(any());
    }
}
