package com.examradar.backend.controller;

import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.NoticeDate;
import com.examradar.backend.domain.NoticeVersion;
import com.examradar.backend.domain.enums.DateType;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.NoticeDateDto;
import com.examradar.backend.dto.NoticeDto;
import com.examradar.backend.dto.NoticeVersionDto;
import com.examradar.backend.repository.NoticeDateRepository;
import com.examradar.backend.repository.NoticeRepository;
import com.examradar.backend.repository.NoticeVersionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notices")
@Tag(name = "Notices", description = "Official recruitment notices, notifications, and version history")
@Transactional(readOnly = true)
public class NoticeController {

    private final NoticeRepository noticeRepository;
    private final NoticeDateRepository dateRepository;
    private final NoticeVersionRepository versionRepository;

    public NoticeController(
            NoticeRepository noticeRepository,
            NoticeDateRepository dateRepository,
            NoticeVersionRepository versionRepository
    ) {
        this.noticeRepository = noticeRepository;
        this.dateRepository = dateRepository;
        this.versionRepository = versionRepository;
    }

    @GetMapping
    @Operation(summary = "Query verified official notices with filtering by exam, category, and notice type")
    public ApiResponse<List<NoticeDto>> getNotices(
            @RequestParam(required = false) String examId,
            @RequestParam(required = false) NoticeType noticeType,
            @RequestParam(required = false) NoticeStatus status,
            @RequestParam(required = false) String search
    ) {
        List<Notice> notices;
        if (examId != null && !examId.isBlank()) {
            notices = noticeRepository.findByExamId(examId);
        } else if (status != null) {
            notices = noticeRepository.findByStatus(status);
        } else {
            notices = noticeRepository.findAll();
        }

        if (noticeType != null) {
            notices = notices.stream().filter(n -> n.getNoticeType() == noticeType).toList();
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            notices = notices.stream()
                    .filter(n -> n.getTitle().toLowerCase().contains(q) ||
                                 (n.getSummary() != null && n.getSummary().toLowerCase().contains(q)))
                    .toList();
        }

        List<NoticeDto> dtos = notices.stream().map(this::toDto).toList();
        return ApiResponse.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get detailed notice information by UUID")
    public ApiResponse<NoticeDto> getNoticeById(@PathVariable UUID id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notice not found: " + id));
        return ApiResponse.ok(toDto(notice));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get immutable revision history for an official notice")
    public ApiResponse<List<NoticeVersionDto>> getNoticeVersions(@PathVariable UUID id) {
        if (!noticeRepository.existsById(id)) {
            throw new NoSuchElementException("Notice not found: " + id);
        }

        List<NoticeVersion> versions = versionRepository.findByNoticeIdOrderByVersionNumberDesc(id);
        List<NoticeVersionDto> dtos = versions.stream().map(v -> new NoticeVersionDto(
                v.getId(),
                v.getNotice().getId(),
                v.getVersionNumber(),
                v.getContentHash(),
                v.getChangeSummary(),
                v.getCreatedAt()
        )).toList();

        return ApiResponse.ok(dtos);
    }

    private NoticeDto toDto(Notice n) {
        List<NoticeDate> dates = dateRepository.findByNoticeIdOrderByEventDateAsc(n.getId());
        List<NoticeDateDto> dateDtos = dates.stream().map(d -> new NoticeDateDto(
                d.getId(),
                d.getDateType(),
                d.getEventDate(),
                d.getIsTentative(),
                d.getIsRevised(),
                d.getOriginalDateStr(),
                d.getSourceTextSnippet()
        )).toList();

        LocalDate deadline = dates.stream()
                .filter(d -> d.getDateType() == DateType.APPLICATION_DEADLINE)
                .map(NoticeDate::getEventDate)
                .findFirst()
                .orElse(null);

        return new NoticeDto(
                n.getId(),
                n.getExam() != null ? n.getExam().getId() : null,
                n.getExam() != null ? n.getExam().getExamName() : null,
                n.getExam() != null ? n.getExam().getOrganization() : null,
                n.getSource() != null ? n.getSource().getId() : null,
                n.getSource() != null ? n.getSource().getSourceName() : null,
                n.getNoticeType(),
                n.getTitle(),
                n.getSummary(),
                n.getOfficialNoticeUrl(),
                n.getOfficialApplyUrl(),
                n.getStatus(),
                n.getPublishedDate(),
                n.getVerificationStatus(),
                deadline,
                dateDtos,
                n.getSource() != null ? n.getSource().getLastSuccessfulFetch() : null,
                n.getCreatedAt(),
                n.getUpdatedAt()
        );
    }
}
