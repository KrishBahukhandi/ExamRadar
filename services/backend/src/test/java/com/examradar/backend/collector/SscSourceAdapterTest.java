package com.examradar.backend.collector;

import com.examradar.backend.collector.adapter.SscSourceAdapter;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.FetchMethod;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SscSourceAdapterTest {

    @Mock
    private PoliteHttpClient httpClient;

    private static final String SAMPLE_SSC_JSON = """
        {
          "statusCode": "200",
          "statusMessage": "Document Found!",
          "data": [
            {
              "id": "notice-1",
              "headline": "Combined Graduate Level Examination (CGL), 2026: Notice of Examination and Online Application closing on 24-11-2026",
              "examId": "cgl2026",
              "contentType": "notice-boards",
              "createdAt": "2026-10-12T13:17:07.360Z",
              "attachments": [
                {
                  "fileName": "cgl_notice_2026.pdf",
                  "path": "uploads\\\\masterData\\\\NoticeBoards\\\\cgl_notice_2026.pdf"
                }
              ]
            },
            {
              "id": "notice-2",
              "headline": "Combined Higher Secondary (10+2) Level Examination, 2025: Declaration of Final Result",
              "examId": "chsl2025",
              "contentType": "notice-boards",
              "createdAt": "2026-10-07T13:40:47.800Z",
              "attachments": []
            }
          ]
        }
        """;

    @Test
    void testSuccessfulSscNoticeExtraction() throws Exception {
        when(httpClient.get(anyString())).thenReturn(
                new PoliteHttpClient.HttpResponseData(200, SAMPLE_SSC_JSON, "hash-ssc-1", 120)
        );

        SscSourceAdapter adapter = new SscSourceAdapter(httpClient);
        Source source = new Source("src-ssc", "SSC", "SSC Notices", "https://ssc.gov.in",
                SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", null);

        ExtractionResult result = adapter.fetchAndExtract(source, null);

        assertTrue(result.isSuccessful());
        assertEquals("src-ssc", result.sourceId());
        assertEquals(2, result.notices().size());

        ExtractedNoticeDto cglNotice = result.notices().get(0);
        assertEquals("exam-ssc-cgl", cglNotice.examId());
        assertEquals("https://ssc.gov.in/api/attachment/uploads/masterData/NoticeBoards/cgl_notice_2026.pdf", cglNotice.officialNoticeUrl());
        assertEquals(LocalDate.of(2026, 10, 12), cglNotice.publishedDate());

        // Verify deadline extraction
        boolean hasDeadline = cglNotice.importantDates().stream()
                .anyMatch(d -> d.eventDate().equals(LocalDate.of(2026, 11, 24)));
        assertTrue(hasDeadline, "Should extract closing deadline 2026-11-24");

        ExtractedNoticeDto chslNotice = result.notices().get(1);
        assertEquals("exam-ssc-chsl", chslNotice.examId());
        assertEquals(NoticeType.RESULT, chslNotice.noticeType());
    }

    @Test
    void testNoChangeDetection() throws Exception {
        when(httpClient.get(anyString())).thenReturn(
                new PoliteHttpClient.HttpResponseData(200, SAMPLE_SSC_JSON, "hash-ssc-1", 80)
        );

        SscSourceAdapter adapter = new SscSourceAdapter(httpClient);
        Source source = new Source("src-ssc", "SSC", "SSC Notices", "https://ssc.gov.in",
                SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", null);

        ExtractionResult result = adapter.fetchAndExtract(source, "hash-ssc-1");

        assertTrue(result.isSuccessful());
        assertFalse(result.contentChanged());
        assertTrue(result.notices().isEmpty());
    }
}
