package com.examradar.backend.service;

import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.NoticeDate;
import com.examradar.backend.dto.CalendarItemDto;
import com.examradar.backend.repository.NoticeDateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CalendarService {

    private final NoticeDateRepository noticeDateRepository;

    public CalendarService(NoticeDateRepository noticeDateRepository) {
        this.noticeDateRepository = noticeDateRepository;
    }

    public List<CalendarItemDto> getCalendarDates(String examId, LocalDate fromDate, LocalDate toDate, boolean upcomingOnly) {
        LocalDate start = fromDate != null ? fromDate : (upcomingOnly ? LocalDate.now() : LocalDate.of(2024, 1, 1));
        LocalDate end = toDate != null ? toDate : LocalDate.of(2030, 12, 31);

        List<NoticeDate> dates = noticeDateRepository.findCalendarEvents(start, end);

        if (examId != null && !examId.isBlank()) {
            dates = dates.stream()
                    .filter(d -> d.getNotice() != null && d.getNotice().getExam() != null &&
                                 examId.equals(d.getNotice().getExam().getId()))
                    .toList();
        }

        return dates.stream().map(d -> {
            Notice n = d.getNotice();
            return new CalendarItemDto(
                    d.getId(),
                    n != null ? n.getId() : null,
                    n != null && n.getExam() != null ? n.getExam().getId() : null,
                    n != null && n.getExam() != null ? n.getExam().getExamName() : "Exam",
                    n != null && n.getExam() != null ? n.getExam().getOrganization() : "GOI",
                    d.getDateType(),
                    d.getEventDate(),
                    d.getIsTentative(),
                    d.getIsRevised(),
                    n != null ? n.getTitle() : "",
                    n != null ? n.getOfficialNoticeUrl() : "",
                    d.getSourceTextSnippet()
            );
        }).toList();
    }
}
