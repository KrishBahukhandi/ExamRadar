package com.examradar.backend.controller;

import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.CalendarItemDto;
import com.examradar.backend.service.CalendarService;
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

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    @Operation(summary = "Get unified examination dates (deadlines, exam dates, admit cards, answer keys, results)")
    public ApiResponse<List<CalendarItemDto>> getCalendarDates(
            @RequestParam(required = false) String examId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "false") boolean upcomingOnly
    ) {
        return ApiResponse.ok(calendarService.getCalendarDates(examId, fromDate, toDate, upcomingOnly));
    }
}
