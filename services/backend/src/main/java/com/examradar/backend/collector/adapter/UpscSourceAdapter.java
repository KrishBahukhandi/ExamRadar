package com.examradar.backend.collector.adapter;

import com.examradar.backend.collector.*;
import com.examradar.backend.domain.Source;
import com.examradar.backend.domain.enums.DateType;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import com.examradar.backend.domain.enums.VerificationStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adapter for Union Public Service Commission (UPSC) official portal notices.
 * Connects to upsc.gov.in/whats-new and extracts official exam notifications,
 * civil services circulars, admit cards, and application deadlines.
 */
@Component
public class UpscSourceAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(UpscSourceAdapter.class);

    public static final String SOURCE_ID = "src-upsc";
    private static final String UPSC_WHATS_NEW_URL = "https://upsc.gov.in/whats-new";
    private static final String UPSC_BASE_URL = "https://upsc.gov.in";

    private final PoliteHttpClient httpClient;

    public UpscSourceAdapter(PoliteHttpClient httpClient) {
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
            PoliteHttpClient.HttpResponseData response = httpClient.get(UPSC_WHATS_NEW_URL);
            long latency = response.responseTimeMs();

            if (response.statusCode() != 200) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "HTTP error from UPSC portal: " + response.statusCode());
            }

            // Change detection using SHA-256
            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(SOURCE_ID, response.contentHash(), response.statusCode(), latency);
            }

            Document doc = Jsoup.parse(response.body());
            List<ExtractedNoticeDto> notices = extractNoticesFromHtml(doc);

            if (notices.isEmpty()) {
                log.warn("UPSC HTML parsed but 0 notices extracted. Page layout may have changed.");
            }

            return ExtractionResult.success(SOURCE_ID, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Exception fetching UPSC notices: {}", e.getMessage(), e);
            return ExtractionResult.failure(SOURCE_ID, 0, latency,
                    "Exception during UPSC fetch: " + e.getMessage());
        }
    }

    private List<ExtractedNoticeDto> extractNoticesFromHtml(Document doc) {
        List<ExtractedNoticeDto> notices = new ArrayList<>();

        // UPSC uses Drupal view tables or item lists on /whats-new
        Elements items = doc.select(".view-content .views-row, .views-table tbody tr, .item-list ul li");

        for (Element item : items) {
            try {
                Element linkElement = item.selectFirst("a[href]");
                if (linkElement == null) continue;

                String title = linkElement.text().trim();
                if (title.isBlank() || title.length() < 5) continue;

                String href = linkElement.attr("href").trim();
                String officialUrl;
                if (href.startsWith("http")) {
                    officialUrl = href;
                } else if (href.startsWith("/")) {
                    officialUrl = UPSC_BASE_URL + href;
                } else {
                    officialUrl = UPSC_BASE_URL + "/" + href;
                }

                // Date parsing from adjacent date span or text
                Element dateElement = item.selectFirst(".date-display-single, .views-field-created, time, span.date");
                LocalDate pubDate = null;
                if (dateElement != null) {
                    Optional<LocalDate> parsedOpt = DateParserUtil.parseDate(dateElement.text().trim());
                    if (parsedOpt.isPresent()) pubDate = parsedOpt.get();
                }
                if (pubDate == null) {
                    Optional<LocalDate> parsedOpt = DateParserUtil.parseDate(item.text());
                    if (parsedOpt.isPresent()) pubDate = parsedOpt.get();
                }
                if (pubDate == null) {
                    pubDate = LocalDate.now();
                }

                String examId = mapExamId(title);
                NoticeType noticeType = detectNoticeType(title);

                List<ExtractedDateDto> dates = new ArrayList<>();
                dates.add(new ExtractedDateDto(
                        DateType.APPLICATION_START, pubDate, false, false,
                        pubDate.toString(), "Published Date", 0.95
                ));

                // Check for deadline patterns in text
                Optional<LocalDate> deadlineOpt = DateParserUtil.extractDeadlineFromText(title);
                if (deadlineOpt.isPresent() && deadlineOpt.get().isAfter(pubDate)) {
                    dates.add(new ExtractedDateDto(
                            DateType.APPLICATION_DEADLINE, deadlineOpt.get(), false, false,
                            deadlineOpt.get().toString(), "Deadline in announcement", 0.90
                    ));
                }

                String summary = "Official announcement from Union Public Service Commission (UPSC): " + title;
                String contentHash = PoliteHttpClient.computeSha256(title + ":" + officialUrl);

                ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                        examId,
                        SOURCE_ID,
                        noticeType,
                        title,
                        summary,
                        officialUrl,
                        "https://upsconline.nic.in",
                        NoticeStatus.PUBLISHED,
                        VerificationStatus.VERIFIED,
                        pubDate,
                        dates,
                        title,
                        contentHash,
                        "Bachelor's Degree or equivalent from a recognized University.",
                        0.95
                );

                notices.add(noticeDto);
            } catch (Exception ex) {
                log.debug("Skipping unparseable UPSC item: {}", ex.getMessage());
            }
        }

        return notices;
    }

    private String mapExamId(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("civil services") || lower.contains("cse") || lower.contains("ias")) {
            return "exam-upsc-cse";
        } else if (lower.contains("defence academy") || lower.contains("nda") || lower.contains("naval academy")) {
            return "exam-upsc-nda";
        } else if (lower.contains("combined defence") || lower.contains("cds")) {
            return "exam-upsc-cds";
        } else if (lower.contains("armed police") || lower.contains("capf")) {
            return "exam-upsc-capf";
        } else if (lower.contains("engineering services") || lower.contains("ese")) {
            return "exam-upsc-ese";
        }
        return "exam-upsc-cse"; // Fallback to UPSC CSE
    }

    private NoticeType detectNoticeType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("admit card") || lower.contains("e-admit") || lower.contains("hall ticket")) {
            return NoticeType.ADMIT_CARD;
        } else if (lower.contains("result") || lower.contains("cutoff") || lower.contains("marks") || lower.contains("score")) {
            return NoticeType.RESULT;
        } else if (lower.contains("answer key") || lower.contains("representation")) {
            return NoticeType.ANSWER_KEY;
        } else if (lower.contains("corrigendum") || lower.contains("addendum") || lower.contains("revision")) {
            return NoticeType.CORRIGENDUM;
        } else if (lower.contains("postpon")) {
            return NoticeType.POSTPONEMENT;
        }
        return NoticeType.RECRUITMENT;
    }
}
