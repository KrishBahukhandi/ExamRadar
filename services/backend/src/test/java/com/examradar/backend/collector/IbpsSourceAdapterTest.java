package com.examradar.backend.collector;

import com.examradar.backend.collector.adapter.IbpsSourceAdapter;
import com.examradar.backend.domain.enums.NoticeType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class IbpsSourceAdapterTest {

    private final PoliteHttpClient mockHttpClient = mock(PoliteHttpClient.class);
    private final IbpsSourceAdapter adapter = new IbpsSourceAdapter(mockHttpClient);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testParseNoticeItem_resultNotice() throws Exception {
        String jsonStr = """
        {
            "id": 31318,
            "date": "2026-09-30T19:35:40",
            "slug": "notification-crp-clerk-result",
            "link": "https://www.ibps.in/index.php/crp-clerk-result/",
            "title": {"rendered": "Notification Regarding CRP Clerical Provisional Allotment Reserve List"},
            "content": {"rendered": "<p>Provisional allotment list for clerical cadre.</p>"}
        }
        """;

        JsonNode node = objectMapper.readTree(jsonStr);
        ExtractedNoticeDto dto = adapter.parseNoticeItem(node);

        assertNotNull(dto);
        assertEquals("exam-ibps-clerk", dto.examId());
        assertEquals(NoticeType.RESULT, dto.noticeType());
        assertEquals("Notification Regarding CRP Clerical Provisional Allotment Reserve List", dto.title());
        assertEquals(LocalDate.of(2026, 9, 30), dto.publishedDate());
    }

    @Test
    void testParseNoticeItem_recruitmentNoticeWithDeadline() throws Exception {
        String jsonStr = """
        {
            "id": 31319,
            "date": "2026-10-01T10:00:00",
            "slug": "crp-po-recruitment",
            "link": "https://www.ibps.in/index.php/crp-po-recruitment/",
            "title": {"rendered": "Common Recruitment Process for Probationary Officers PO XV"},
            "content": {"rendered": "Online application opens. Last date: 28-10-2026 up to 11:59 PM."}
        }
        """;

        JsonNode node = objectMapper.readTree(jsonStr);
        ExtractedNoticeDto dto = adapter.parseNoticeItem(node);

        assertNotNull(dto);
        assertEquals("exam-ibps-po", dto.examId());
        assertEquals(NoticeType.RECRUITMENT, dto.noticeType());
        assertEquals(2, dto.importantDates().size()); // Start date + Deadline
    }
}
