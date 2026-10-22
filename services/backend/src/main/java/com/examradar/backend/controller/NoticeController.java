package com.examradar.backend.controller;

import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.NoticeDto;
import com.examradar.backend.dto.NoticeVersionDto;
import com.examradar.backend.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notices")
@Tag(name = "Notices", description = "Official recruitment notices, notifications, and version history")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    @Operation(summary = "Query verified official notices with filtering by exam, category, and notice type")
    public ApiResponse<List<NoticeDto>> getNotices(
            @RequestParam(required = false) String examId,
            @RequestParam(required = false) NoticeType noticeType,
            @RequestParam(required = false) NoticeStatus status,
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(noticeService.getNotices(examId, noticeType, status, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed notice information by UUID")
    public ApiResponse<NoticeDto> getNoticeById(@PathVariable UUID id) {
        return ApiResponse.ok(noticeService.getNoticeById(id));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get immutable revision history for an official notice")
    public ApiResponse<List<NoticeVersionDto>> getNoticeVersions(@PathVariable UUID id) {
        return ApiResponse.ok(noticeService.getNoticeVersions(id));
    }
}
