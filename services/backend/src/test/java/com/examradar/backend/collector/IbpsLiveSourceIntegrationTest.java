package com.examradar.backend.collector;

import com.examradar.backend.collector.adapter.IbpsSourceAdapter;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.FetchMethod;
import com.examradar.backend.domain.enums.SourceType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
class IbpsLiveSourceIntegrationTest {

    @Test
    void testLiveIbpsSourceFetchAndExtraction() {
        PoliteHttpClient httpClient = new PoliteHttpClient(
                "ExamRadarBot/1.0 (+https://examradar.in/bot; bot@examradar.in)", 15000
        );
        IbpsSourceAdapter adapter = new IbpsSourceAdapter(httpClient);

        Source source = new Source(
                "src-ibps", "IBPS", "IBPS Live Test", "https://www.ibps.in",
                SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", "Live Integration Test"
        );

        ExtractionResult result = adapter.fetchAndExtract(source, null);

        assertNotNull(result);
        System.out.println("Live IBPS Fetch Status: " + result.httpStatus());
        System.out.println("Live IBPS Content Hash: " + result.contentHash());
        System.out.println("Live IBPS Notices Extracted: " + result.notices().size());

        assertTrue(result.isSuccessful(), "Live fetch to IBPS official portal must succeed: " + result.errorMessage());
        assertEquals(200, result.httpStatus());
        assertNotNull(result.contentHash());
        assertFalse(result.contentHash().isBlank());
        assertFalse(result.notices().isEmpty(), "At least one official notice must be parsed from IBPS");

        ExtractedNoticeDto firstNotice = result.notices().get(0);
        System.out.println("Sample Live Notice Title: " + firstNotice.title());
        System.out.println("Sample Live Notice URL: " + firstNotice.officialNoticeUrl());
        System.out.println("Sample Live Notice Published Date: " + firstNotice.publishedDate());

        assertNotNull(firstNotice.title());
        assertNotNull(firstNotice.officialNoticeUrl());
        assertTrue(firstNotice.officialNoticeUrl().startsWith("https://www.ibps.in"));
    }
}
