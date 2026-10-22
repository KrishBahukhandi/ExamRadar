package com.examradar.backend.collector;

import com.examradar.backend.collector.adapter.UpscSourceAdapter;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.FetchMethod;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.SourceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpscSourceAdapterTest {

    @Mock
    private PoliteHttpClient httpClient;

    private static final String SAMPLE_UPSC_HTML = """
        <!DOCTYPE html>
        <html>
        <head><title>What's New | UPSC</title></head>
        <body>
        <div class="view-content">
            <div class="views-row">
                <span class="views-field-title">
                    <a href="/sites/default/files/Notice-CSE-2026-Eng.pdf">Civil Services (Preliminary) Examination, 2026: Notice & Online Application closing on 24-11-2026</a>
                </span>
                <span class="views-field-created"><span class="date-display-single">14-10-2026</span></span>
            </div>
            <div class="views-row">
                <span class="views-field-title">
                    <a href="/sites/default/files/eAdmit-NDA-II-2026.pdf">e-Admit Card: National Defence Academy & Naval Academy Examination (II), 2026</a>
                </span>
                <span class="views-field-created"><span class="date-display-single">10-10-2026</span></span>
            </div>
            <div class="views-row">
                <span class="views-field-title">
                    <a href="/sites/default/files/FinalResult-CDS-I-2026.pdf">Final Result: Combined Defence Services Examination (I), 2026</a>
                </span>
                <span class="views-field-created"><span class="date-display-single">05-10-2026</span></span>
            </div>
        </div>
        </body>
        </html>
        """;

    @Test
    void testSuccessfulUpscNoticeExtraction() throws Exception {
        when(httpClient.get(anyString())).thenReturn(
                new PoliteHttpClient.HttpResponseData(200, SAMPLE_UPSC_HTML, "hash-upsc-1", 150)
        );

        UpscSourceAdapter adapter = new UpscSourceAdapter(httpClient);
        Source source = new Source("src-upsc", "UPSC", "UPSC Active Notice Board",
                "https://upsc.gov.in", SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", "Notes");

        ExtractionResult result = adapter.fetchAndExtract(source, null);

        assertTrue(result.isSuccessful());
        assertEquals(200, result.httpStatus());
        assertEquals(3, result.notices().size());

        // 1. Civil Services Examination Notice
        ExtractedNoticeDto cseNotice = result.notices().get(0);
        assertEquals("exam-upsc-cse", cseNotice.examId());
        assertEquals(NoticeType.RECRUITMENT, cseNotice.noticeType());
        assertTrue(cseNotice.title().contains("Civil Services"));
        assertEquals("https://upsc.gov.in/sites/default/files/Notice-CSE-2026-Eng.pdf", cseNotice.officialNoticeUrl());
        assertTrue(cseNotice.importantDates().size() >= 2);

        // 2. NDA Admit Card
        ExtractedNoticeDto ndaNotice = result.notices().get(1);
        assertEquals("exam-upsc-nda", ndaNotice.examId());
        assertEquals(NoticeType.ADMIT_CARD, ndaNotice.noticeType());

        // 3. CDS Result
        ExtractedNoticeDto cdsNotice = result.notices().get(2);
        assertEquals("exam-upsc-cds", cdsNotice.examId());
        assertEquals(NoticeType.RESULT, cdsNotice.noticeType());
    }

    @Test
    void testNoChangeDetection() throws Exception {
        when(httpClient.get(anyString())).thenReturn(
                new PoliteHttpClient.HttpResponseData(200, SAMPLE_UPSC_HTML, "hash-upsc-same", 120)
        );

        UpscSourceAdapter adapter = new UpscSourceAdapter(httpClient);
        Source source = new Source("src-upsc", "UPSC", "UPSC Notice Board",
                "https://upsc.gov.in", SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0.0", "Notes");

        ExtractionResult result = adapter.fetchAndExtract(source, "hash-upsc-same");

        assertTrue(result.isSuccessful());
        assertFalse(result.contentChanged());
        assertTrue(result.notices().isEmpty());
    }
}
