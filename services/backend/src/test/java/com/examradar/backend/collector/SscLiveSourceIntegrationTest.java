package com.examradar.backend.collector;

import com.examradar.backend.collector.adapter.SscSourceAdapter;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.FetchMethod;
import com.examradar.backend.domain.enums.SourceType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class SscLiveSourceIntegrationTest {

    @Test
    void testLiveSscSourceFetchAndExtraction() {
        PoliteHttpClient httpClient = new PoliteHttpClient(
                "ExamRadarBot/1.0 (+https://examradar.in/bot; bot@examradar.in)", 15000
        );
        SscSourceAdapter adapter = new SscSourceAdapter(httpClient);

        Source source = new Source(
                "src-ssc", "Staff Selection Commission", "SSC Live Test", "https://ssc.gov.in",
                SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", "Live Integration Test"
        );

        ExtractionResult result = adapter.fetchAndExtract(source, null);

        assertNotNull(result);
        System.out.println("Live SSC Fetch Status: " + result.httpStatus());
        System.out.println("Live SSC Content Hash: " + result.contentHash());
        System.out.println("Live SSC Notices Extracted: " + result.notices().size());

        assertTrue(result.isSuccessful(), "Live fetch to SSC portal must succeed: " + result.errorMessage());
        assertEquals(200, result.httpStatus());
        assertNotNull(result.contentHash());
        assertFalse(result.contentHash().isBlank());
        assertFalse(result.notices().isEmpty(), "At least one notice must be extracted from SSC live notice board");

        ExtractedNoticeDto firstNotice = result.notices().get(0);
        System.out.println("Sample Live SSC Notice Title: " + firstNotice.title());
        System.out.println("Sample Live SSC Notice URL: " + firstNotice.officialNoticeUrl());
        System.out.println("Sample Live SSC Notice Exam ID: " + firstNotice.examId());

        assertNotNull(firstNotice.title());
        assertNotNull(firstNotice.officialNoticeUrl());
        assertTrue(firstNotice.officialNoticeUrl().startsWith("https://ssc.gov.in"));
    }
}
