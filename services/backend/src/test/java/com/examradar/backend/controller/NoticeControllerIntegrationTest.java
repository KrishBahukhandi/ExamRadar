package com.examradar.backend.controller;

import com.examradar.backend.domain.Exam;
import com.examradar.backend.domain.ExamCategory;
import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import com.examradar.backend.repository.ExamCategoryRepository;
import com.examradar.backend.repository.ExamRepository;
import com.examradar.backend.repository.NoticeRepository;
import com.examradar.backend.repository.SourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NoticeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamCategoryRepository categoryRepository;

    @Autowired
    private SourceRepository sourceRepository;

    private Notice savedNotice;

    @BeforeEach
    void setUp() {
        ExamCategory cat = new ExamCategory("cat-notice-test", "Banking Category", "banking-test", "Desc", 1);
        categoryRepository.save(cat);

        Exam exam = new Exam("exam-notice-po", cat, "IBPS", "IBPS PO 2026", "IBPS PO", "https://ibps.in", "desc");
        examRepository.save(exam);

        Source source = new Source("src-notice-test", "IBPS", "IBPS Feed", "https://ibps.in",
                com.examradar.backend.domain.enums.SourceType.HTML_SCRAPE,
                com.examradar.backend.domain.enums.FetchMethod.HTTP_GET, 60, "1.0", null);
        sourceRepository.save(source);

        Notice notice = new Notice(
                exam, source, NoticeType.RECRUITMENT, "IBPS PO/MT-XIV Official Notification",
                "Common Recruitment Process for Probationary Officers in 11 participating banks",
                "https://ibps.in/po-xiv-circular", "https://ibpsonline.ibps.in",
                NoticeStatus.PUBLISHED, LocalDate.now(), VerificationStatus.VERIFIED
        );
        savedNotice = noticeRepository.save(notice);
    }

    @Test
    void testGetNoticesPublic() throws Exception {
        mockMvc.perform(get("/api/v1/notices")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void testGetNoticeById() throws Exception {
        mockMvc.perform(get("/api/v1/notices/" + savedNotice.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(savedNotice.getId().toString()))
                .andExpect(jsonPath("$.data.title").value("IBPS PO/MT-XIV Official Notification"))
                .andExpect(jsonPath("$.data.organization").value("IBPS"));
    }

    @Test
    void testGetNoticeNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/notices/" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
