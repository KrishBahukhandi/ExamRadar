package com.examradar.backend.controller;

import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.NoticeDate;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.CalendarItemDto;
import com.examradar.backend.repository.NoticeDateRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/calendar")
@Tag(name = "Calendar", description = "Unified government examination timeline and important dates")
public class CalendarController {

    private final NoticeDateRepository noticeDateRepository;

    public CalendarController(NoticeDateRepository noticeDateRepository) {
        this.noticeDateRepository = noticeDateRepository;
    }

    @GetMapping
    @Operation(summary = "Get unified examination dates (deadlines, exam dates, admit cards, answer keys, results)")
    public ApiResponse<List<CalendarItemDto>> getCalendarDates(
            @RequestParam(required = false) String examId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean upcomingOnly
    ) {
        LocalDate start = fromDate != null ? fromDate : (upcomingOnly ? LocalDate.now() : LocalDate.of(2024, 1, 1));
        LocalDate end = toDate != null ? toDate : LocalDate.of(2030, 12, 31);

        List<NoticeDate> dates = noticeDateRepository.findByEventDateBetweenOrderByEventDateAsc(start, end);

        if (examId != null && !examId.isBlank()) {
            dates = dates.stream()
                    .filter(d -> d.getNotice() != null && d.getNotice().getExam() != null &&
                                 examId.equals(d.getNotice().getExam().getId()))
                    .toList();
        }

        List<CalendarItemDto> items = dates.stream().map(d -> {
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
                    n != null ? n.getOfficialApplyUrl() : ""
            );
        }).toList();

        return ApiResponse.ok(items);
    }
}
