package com.examradar.backend.collector.adapter;

import com.examradar.backend.collector.*;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.DateType;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

import java.time.LocalDate
;import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class IbpsSourceAdapter implements SourceAdapter {

    public static final String SOURCE_ID = "src-ibps";
    private static final String API_URL = "https://www.ibps.in/index.php/wp-json/wp/v2/pages?search=notification&per_page=10";

    private final PoliteHttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IbpsSourceAdapter(PoliteHttpClient httpClient) {
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
                        "HTTP error from IBPS portal: " + response.statusCode());
            }

            // Change detection using SHA-256
            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(SOURCE_ID, response.contentHash(), response.statusCode(), latency);
            }

            String body = response.body();
            int jsonStartIndex = body.indexOf('[');
            int jsonEndIndex = body.lastIndexOf(']');

            if (jsonStartIndex == -1 || jsonEndIndex == -1 || jsonEndIndex <= jsonStartIndex) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "Invalid JSON format received from IBPS WP API");
            }

            String cleanJson = body.substring(jsonStartIndex, jsonEndIndex + 1);
            JsonNode rootNode = objectMapper.readTree(cleanJson);

            List<ExtractedNoticeDto> notices = new ArrayList<>();

            if (rootNode.isArray()) {
                for (JsonNode item : rootNode) {
                    ExtractedNoticeDto noticeDto = parseNoticeItem(item);
                    if (noticeDto != null) {
                        notices.add(noticeDto);
                    }
                }
            }

            return ExtractionResult.success(SOURCE_ID, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            return ExtractionResult.failure(SOURCE_ID, 0, latency, "Exception during IBPS fetch: " + e.getMessage());
        }
    }

    public ExtractedNoticeDto parseNoticeItem(JsonNode item) {
        try {
            String rawTitle = item.path("title").path("rendered").asText("");
            String cleanTitle = Jsoup.parse(rawTitle).text().trim();
            if (cleanTitle.isBlank()) return null;

            String link = item.path("link").asText("https://www.ibps.in");
            String dateStr = item.path("date").asText("");
            LocalDate publishedDate = null;
            if (!dateStr.isBlank()) {
                try {
                    publishedDate = LocalDate.parse(dateStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {}
            }

            String rawContent = item.path("content").path("rendered").asText("");
            String plainText = Jsoup.parse(rawContent).text();
            String summary = plainText.length() > 300 ? plainText.substring(0, 297) + "..." : plainText;
            if (summary.isBlank()) {
                summary = cleanTitle;
            }

            // Identify Exam ID
            String examId = resolveExamId(cleanTitle);

            // Identify Notice Type
            NoticeType noticeType = resolveNoticeType(cleanTitle);

            // Extract Important Dates
            List<ExtractedDateDto> importantDates = new ArrayList<>();
            if (publishedDate != null) {
                importantDates.add(new ExtractedDateDto(
                        DateType.APPLICATION_START, publishedDate, false, false,
                        publishedDate.toString(), "Official Gazette Publication Date", 1.0
                ));
            }

            Optional<LocalDate> deadlineOpt = DateParserUtil.extractDeadlineFromText(plainText);
            deadlineOpt.ifPresent(deadline -> importantDates.add(new ExtractedDateDto(
                    DateType.APPLICATION_DEADLINE, deadline, false, false,
                    deadline.toString(), "Application deadline extracted from notice body", 0.95
            )));

            // Compute hash for deduplication
            String noticeHash = PoliteHttpClient.computeSha256(cleanTitle + link + dateStr);

            return new ExtractedNoticeDto(
                    examId,
                    SOURCE_ID,
                    noticeType,
                    cleanTitle,
                    summary,
                    link,
                    link.contains("ibpsonline") ? link : null,
                    NoticeStatus.PUBLISHED,
                    VerificationStatus.VERIFIED,
                    publishedDate,
                    importantDates,
                    plainText,
                    noticeHash,
                    "Degree (Graduation) in any discipline from a recognized University in India.",
                    0.95
            );
        } catch (Exception e) {
            return null;
        }
    }

    private String resolveExamId(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("clerk") || lower.contains("clerical")) return "exam-ibps-clerk";
        if (lower.contains("spl") || lower.contains("specialist")) return "exam-ibps-so";
        return "exam-ibps-po"; // Default IBPS PO/MT
    }

    private NoticeType resolveNoticeType(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("result") || lower.contains("allotment") || lower.contains("score")) return NoticeType.RESULT;
        if (lower.contains("admit card") || lower.contains("call letter")) return NoticeType.ADMIT_CARD;
        if (lower.contains("corrigendum") || lower.contains("revision") || lower.contains("extension")) return NoticeType.CORRIGENDUM;
        if (lower.contains("answer key")) return NoticeType.ANSWER_KEY;
        if (lower.contains("postpon")) return NoticeType.POSTPONEMENT;
        return NoticeType.RECRUITMENT;
    }
}
