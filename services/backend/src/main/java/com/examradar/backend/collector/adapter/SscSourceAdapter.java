package com.examradar.backend.collector.adapter;

import com.examradar.backend.collector.*;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.DateType;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapter for Staff Selection Commission (SSC) official portal notices.
 * Connects to the official SSC public notice-boards API and extracts
 * circulars, admit-card releases, answer keys, results, and PDF attachments.
 */
@Component
public class SscSourceAdapter implements SourceAdapter {

    public static final String SOURCE_ID = "src-ssc";
    private static final String API_URL = "https://ssc.gov.in/api/general-website/portal/notice-boards?" +
            "page=1&limit=15&contentType=notice-boards&key=createdAt&order=DESC&isAttachment=true&language=english" +
            "&attributes=id,headline,examId,contentType,redirectUrl,startDate,endDate,language,createdAt";
    private static final String ATTACHMENT_BASE_URL = "https://ssc.gov.in/api/attachment/";

    private final PoliteHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SscSourceAdapter(PoliteHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    public ExtractionResult fetchAndExtract(Source source, String lastContentHash) {
        long startTime = System.currentTimeMillis();
        try {
            PoliteHttpClient.HttpResponseData response = httpClient.get(API_URL);
            long latency = response.responseTimeMs();

            if (response.statusCode() != 200) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "HTTP " + response.statusCode() + " from SSC official portal");
            }

            // Change detection via content SHA-256
            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(SOURCE_ID, response.contentHash(), response.statusCode(), latency);
            }

            JsonNode rootNode = objectMapper.readTree(response.body());
            JsonNode dataNode = rootNode.path("data");

            if (!dataNode.isArray()) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "Unexpected SSC JSON payload: missing data array");
            }

            List<ExtractedNoticeDto> notices = new ArrayList<>();
            for (JsonNode item : dataNode) {
                ExtractedNoticeDto noticeDto = parseNoticeItem(item);
                if (noticeDto != null) {
                    notices.add(noticeDto);
                }
            }

            return ExtractionResult.success(SOURCE_ID, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return ExtractionResult.failure(SOURCE_ID, 0, latency,
                    "Exception during SSC fetch: " + e.getMessage());
        }
    }

    public ExtractedNoticeDto parseNoticeItem(JsonNode item) {
        try {
            String noticeId = item.path("id").asText("");
            String headline = item.path("headline").asText("").trim();
            if (headline.isBlank()) return null;

            String sscExamCode = item.path("examId").asText("");
            String createdAtStr = item.path("createdAt").asText("");
            LocalDate publishedDate = null;
            if (!createdAtStr.isBlank()) {
                try {
                    OffsetDateTime odt = OffsetDateTime.parse(createdAtStr);
                    publishedDate = odt.toLocalDate();
                } catch (Exception ignored) {
                    try {
                        publishedDate = LocalDate.parse(createdAtStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                    } catch (Exception ignored2) {}
                }
            }

            // Determine Attachment or Document Link
            String officialNoticeUrl = "https://ssc.gov.in/notices";
            JsonNode attachments = item.path("attachments");
            if (attachments.isArray() && !attachments.isEmpty()) {
                String path = attachments.get(0).path("path").asText("");
                if (!path.isBlank()) {
                    String normalizedPath = path.replace('\\', '/');
                    if (normalizedPath.startsWith("/")) {
                        normalizedPath = normalizedPath.substring(1);
                    }
                    officialNoticeUrl = ATTACHMENT_BASE_URL + normalizedPath;
                }
            }

            // Resolve Exam ID
            String resolvedExamId = resolveExamId(headline, sscExamCode);

            // Resolve Notice Type
            NoticeType noticeType = resolveNoticeType(headline);

            // Extract Dates
            List<ExtractedDateDto> importantDates = new ArrayList<>();
            if (publishedDate != null) {
                importantDates.add(new ExtractedDateDto(
                        DateType.APPLICATION_START, publishedDate, false, false,
                        publishedDate.toString(), "SSC Notice Publication Date", 1.0
                ));
            }

            // Extract potential dates mentioned in headline
            Optional<LocalDate> deadlineOpt = DateParserUtil.extractDeadlineFromText(headline);
            deadlineOpt.ifPresent(deadline -> importantDates.add(new ExtractedDateDto(
                    DateType.APPLICATION_DEADLINE, deadline, false, false,
                    deadline.toString(), "Extracted from SSC notice headline", 0.90
            )));

            // Compute notice content hash
            String noticeHash = PoliteHttpClient.computeSha256(noticeId + headline + createdAtStr);

            String eligibility = resolvedExamId.equals("exam-ssc-cgl")
                    ? "Bachelor's Degree from a recognized University or equivalent."
                    : "12th Standard or equivalent examination from a recognized Board or University.";

            return new ExtractedNoticeDto(
                    resolvedExamId,
                    SOURCE_ID,
                    noticeType,
                    headline,
                    headline,
                    officialNoticeUrl,
                    "https://ssc.gov.in",
                    NoticeStatus.PUBLISHED,
                    VerificationStatus.VERIFIED,
                    publishedDate,
                    importantDates,
                    headline,
                    noticeHash,
                    eligibility,
                    0.95
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveExamId(String headline, String sscExamCode) {
        String lower = headline.toLowerCase();
        String codeLower = sscExamCode.toLowerCase();

        if (lower.contains("graduate level") || lower.contains("cgl") || codeLower.contains("cgl")) {
            return "exam-ssc-cgl";
        }
        if (lower.contains("higher secondary") || lower.contains("chsl") || codeLower.contains("chsl")) {
            return "exam-ssc-chsl";
        }
        if (lower.contains("sub-inspector") || lower.contains("cpo") || codeLower.contains("cpo")) {
            return "exam-ssc-cpo";
        }
        if (lower.contains("junior engineer") || lower.contains("je") || codeLower.contains("je")) {
            return "exam-ssc-je";
        }
        // Fallback default for central SSC notices
        return "exam-ssc-cgl";
    }

    private NoticeType resolveNoticeType(String headline) {
        String lower = headline.toLowerCase();
        if (lower.contains("cancellation") || lower.contains("cancelled")) {
            return NoticeType.CORRIGENDUM;
        }
        if (lower.contains("postpon")) {
            return NoticeType.POSTPONEMENT;
        }
        if (lower.contains("result") || lower.contains("marks") || lower.contains("score")) {
            return NoticeType.RESULT;
        }
        if (lower.contains("answer key") || lower.contains("response sheet")) {
            return NoticeType.ANSWER_KEY;
        }
        if (lower.contains("admit card") || lower.contains("call letter") || lower.contains("hall ticket")
                || lower.contains("pet") || lower.contains("pst") || lower.contains("shortlisting")) {
            return NoticeType.ADMIT_CARD;
        }
        if (lower.contains("corrigendum") || lower.contains("revised") || lower.contains("extension")) {
            return NoticeType.CORRIGENDUM;
        }
        return NoticeType.RECRUITMENT;
    }
}
