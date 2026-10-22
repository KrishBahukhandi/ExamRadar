package com.examradar.backend.service;

import com.examradar.backend.domain.Exam;
import com.examradar.backend.domain.ExamCategory;
import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.FetchMethod;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.SourceType;
import com.examradar.backend.domain.enums.VerificationStatus;
import com.examradar.backend.dto.NoticeDto;
import com.examradar.backend.repository.NoticeDateRepository;
import com.examradar.backend.repository.NoticeRepository;
import com.examradar.backend.repository.NoticeVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeDateRepository dateRepository;

    @Mock
    private NoticeVersionRepository versionRepository;

    @InjectMocks
    private NoticeService noticeService;

    private Notice testNotice;

    @BeforeEach
    void setUp() {
        ExamCategory cat = new ExamCategory("cat-ssc", "SSC", "ssc", "Desc", 1);
        Exam exam = new Exam("exam-ssc-cgl", cat, "SSC", "Combined Graduate Level", "CGL", "https://ssc.gov.in", "desc");
        Source source = new Source("src-ssc", "SSC", "SSC Portal", "https://ssc.gov.in", SourceType.HTML_SCRAPE, FetchMethod.HTTP_GET, 60, "1.0", null);

        testNotice = new Notice(
                exam, source, NoticeType.RECRUITMENT, "SSC CGL 2025 Notification",
                "Official recruitment notification for SSC CGL 2025",
                "https://ssc.gov.in/notice", "https://ssc.gov.in/apply",
                NoticeStatus.PUBLISHED, LocalDate.now(), VerificationStatus.VERIFIED
        );
    }

    @Test
    void testGetNoticesAll() {
        when(noticeRepository.findByStatusOrderByPublishedDateDesc(NoticeStatus.PUBLISHED)).thenReturn(List.of(testNotice));
        when(dateRepository.findByNoticeIds(any())).thenReturn(Collections.emptyList());

        List<NoticeDto> results = noticeService.getNotices(null, null, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).title()).isEqualTo("SSC CGL 2025 Notification");
    }

    @Test
    void testGetNoticeByIdFound() {
        UUID id = testNotice.getId();
        when(noticeRepository.findById(id)).thenReturn(Optional.of(testNotice));
        when(dateRepository.findByNoticeIdOrderByEventDateAsc(id)).thenReturn(Collections.emptyList());

        NoticeDto dto = noticeService.getNoticeById(id);

        assertThat(dto.id()).isEqualTo(id);
        assertThat(dto.examName()).isEqualTo("Combined Graduate Level");
        assertThat(dto.organization()).isEqualTo("SSC");
    }

    @Test
    void testGetNoticeByIdNotFound() {
        UUID randomId = UUID.randomUUID();
        when(noticeRepository.findById(randomId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noticeService.getNoticeById(randomId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Notice not found");
    }
}
