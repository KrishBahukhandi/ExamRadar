package com.examradar.backend.service;

import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.NoticeDate;
import com.examradar.backend.domain.NoticeVersion;
import com.examradar.backend.domain.enums.DateType;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.dto.NoticeDateDto;
import com.examradar.backend.dto.NoticeDto;
import com.examradar.backend.dto.NoticeVersionDto;
import com.examradar.backend.repository.NoticeDateRepository;
import com.examradar.backend.repository.NoticeRepository;
import com.examradar.backend.repository.NoticeVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeDateRepository dateRepository;
    private final NoticeVersionRepository versionRepository;

    public NoticeService(
            NoticeRepository noticeRepository,
            NoticeDateRepository dateRepository,
            NoticeVersionRepository versionRepository
    ) {
        this.noticeRepository = noticeRepository;
        this.dateRepository = dateRepository;
        this.versionRepository = versionRepository;
    }

    @org.springframework.cache.annotation.Cacheable(value = com.examradar.backend.config.CacheConfig.CACHE_NOTICES, key = "{#examId, #noticeType, #status, #search}")
    public List<NoticeDto> getNotices(String examId, NoticeType noticeType, NoticeStatus status, String search) {
        NoticeStatus effectiveStatus = (status != null) ? status : NoticeStatus.PUBLISHED;
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String examParam = (examId != null && !examId.isBlank()) ? examId.trim() : null;

        List<Notice> notices;
        if (examParam == null && noticeType == null && searchParam == null && status == null) {
            notices = noticeRepository.findByStatusOrderByPublishedDateDesc(effectiveStatus);
        } else {
            notices = noticeRepository.findFiltered(examParam, noticeType, effectiveStatus, searchParam);
        }

        if (notices.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = notices.stream().map(Notice::getId).toList();
        List<NoticeDate> allDates = dateRepository.findByNoticeIds(ids);
        Map<UUID, List<NoticeDate>> datesByNoticeId = allDates.stream()
                .collect(Collectors.groupingBy(d -> d.getNotice().getId()));

        return notices.stream().map(n -> toDto(n, datesByNoticeId.getOrDefault(n.getId(), List.of()))).toList();
    }

    public Page<NoticeDto> getNotices(String examId, NoticeType noticeType, NoticeStatus status, String search, Pageable pageable) {
        NoticeStatus effectiveStatus = (status != null) ? status : NoticeStatus.PUBLISHED;
        String searchParam = (search != null && !search.isBlank()) ? search.trim() : null;
        String examParam = (examId != null && !examId.isBlank()) ? examId.trim() : null;

        Page<Notice> page;
        if (examParam == null && noticeType == null && searchParam == null && (status == null || status == NoticeStatus.PUBLISHED)) {
            page = noticeRepository.findPublishedNotices(null, null, pageable);
        } else {
            page = noticeRepository.findFiltered(examParam, noticeType, effectiveStatus, searchParam, pageable);
        }
        if (page.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> ids = page.getContent().stream().map(Notice::getId).toList();
        List<NoticeDate> allDates = dateRepository.findByNoticeIds(ids);
        Map<UUID, List<NoticeDate>> datesByNoticeId = allDates.stream()
                .collect(Collectors.groupingBy(d -> d.getNotice().getId()));

        return page.map(n -> toDto(n, datesByNoticeId.getOrDefault(n.getId(), List.of())));
    }

    @org.springframework.cache.annotation.Cacheable(value = com.examradar.backend.config.CacheConfig.CACHE_NOTICES, key = "#id")
    public NoticeDto getNoticeById(UUID id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Notice not found: " + id));
        return toDto(notice);
    }

    public List<NoticeVersionDto> getNoticeVersions(UUID id) {
        if (!noticeRepository.existsById(id)) {
            throw new NoSuchElementException("Notice not found: " + id);
        }

        List<NoticeVersion> versions = versionRepository.findByNoticeIdOrderByVersionNumberDesc(id);
        return versions.stream().map(v -> new NoticeVersionDto(
                v.getId(),
                v.getNotice().getId(),
                v.getVersionNumber(),
                v.getContentHash(),
                v.getChangeSummary(),
                v.getCreatedAt()
        )).toList();
    }

    public NoticeDto toDto(Notice n) {
        List<NoticeDate> dates = dateRepository.findByNoticeIdOrderByEventDateAsc(n.getId());
        return toDto(n, dates);
    }

    public NoticeDto toDto(Notice n, List<NoticeDate> dates) {
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
                n.getUpdatedAt(),
                n.getTotalVacancies(),
                n.getVacancyBreakdownJson(),
                n.getPayScaleSummary(),
                n.getApplicationFeeGeneral(),
                n.getApplicationFeeReserved(),
                n.getFeeExemptionDetails(),
                n.getAgeMin(),
                n.getAgeMax(),
                n.getAgeCutoffDate(),
                n.getAgeRelaxationSummary(),
                n.getEducationalQualification(),
                n.getHasCorrigendum(),
                n.getCorrigendumSummary(),
                n.getPhotoSpecifications(),
                n.getSignatureSpecifications(),
                n.getFeePaymentDeadline(),
                n.getTitleHi(),
                n.getSummaryHi(),
                n.getExamStages(),
                n.getNegativeMarking(),
                n.getExamMode(),
                n.getSyllabusUrl()
        );
    }
}
