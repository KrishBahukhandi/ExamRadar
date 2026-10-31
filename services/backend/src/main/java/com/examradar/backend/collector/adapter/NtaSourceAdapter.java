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
 * Adapter for National Testing Agency (NTA) public circulars and exam announcements.
 * Connects to nta.ac.in and parses public notices for JEE Main, NEET UG, CUET, and UGC NET.
 */
@Component
public class NtaSourceAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(NtaSourceAdapter.class);

    public static final String SOURCE_ID = "src-nta";
    private static final String NTA_NOTICES_URL = "https://nta.ac.in/Notice";
    private static final String NTA_BASE_URL = "https://nta.ac.in";

    private final PoliteHttpClient httpClient;

    public NtaSourceAdapter(PoliteHttpClient httpClient) {
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
            PoliteHttpClient.HttpResponseData response = httpClient.get(NTA_NOTICES_URL);
            long latency = response.responseTimeMs();

            if (response.statusCode() != 200) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "HTTP error from NTA portal: " + response.statusCode());
            }

            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(SOURCE_ID, response.contentHash(), response.statusCode(), latency);
            }

            Document doc = Jsoup.parse(response.body());
            List<ExtractedNoticeDto> notices = extractNoticesFromHtml(doc);

            if (notices.isEmpty()) {
                log.warn("NTA HTML parsed but 0 notices extracted. Portal layout may have changed.");
            }

            return ExtractionResult.success(SOURCE_ID, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Exception fetching NTA notices: {}", e.getMessage(), e);
            return ExtractionResult.failure(SOURCE_ID, 0, latency,
                    "Exception during NTA fetch: " + e.getMessage());
        }
    }

    private List<ExtractedNoticeDto> extractNoticesFromHtml(Document doc) {
        List<ExtractedNoticeDto> notices = new ArrayList<>();

        // NTA table rows on /Notice
        Elements rows = doc.select("table tbody tr, .notice-board tr, .archive-table tr, .views-table tr");

        for (Element row : rows) {
            try {
                Element linkElement = row.selectFirst("a[href]");
                if (linkElement == null) continue;

                String title = linkElement.text().trim();
                if (title.isBlank() || title.length() < 6) {
                    // Try adjacent text if link is just 'Download' or 'Read More'
                    title = row.select("td").text().trim();
                }
                if (title.isBlank() || title.length() < 6) continue;

                String href = linkElement.attr("href").trim();
                String officialUrl;
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    officialUrl = href;
                } else if (href.startsWith("/")) {
                    officialUrl = NTA_BASE_URL + href;
                } else {
                    officialUrl = NTA_BASE_URL + "/" + href;
                }

                // Date parsing from date column or adjacent text
                LocalDate pubDate = null;
                Elements cells = row.select("td");
                for (Element cell : cells) {
                    Optional<LocalDate> dOpt = DateParserUtil.parseDate(cell.text().trim());
                    if (dOpt.isPresent()) {
                        pubDate = dOpt.get();
                        break;
                    }
                }
                if (pubDate == null) {
                    pubDate = DateParserUtil.parseDate(row.text()).orElse(LocalDate.now());
                }

                String examId = mapExamId(title);
                NoticeType noticeType = detectNoticeType(title);

                List<ExtractedDateDto> dates = new ArrayList<>();
                dates.add(new ExtractedDateDto(
                        DateType.APPLICATION_START, pubDate, false, false,
                        pubDate.toString(), "Published Date", 0.95
                ));

                Optional<LocalDate> deadlineOpt = DateParserUtil.extractDeadlineFromText(title);
                if (deadlineOpt.isPresent() && deadlineOpt.get().isAfter(pubDate)) {
                    dates.add(new ExtractedDateDto(
                            DateType.APPLICATION_DEADLINE, deadlineOpt.get(), false, false,
                            deadlineOpt.get().toString(), "Deadline in announcement", 0.90
                    ));
                }

                String summary = "Official announcement from National Testing Agency (NTA): " + title;
                String titleHi = "राष्ट्रीय परीक्षा एजेंसी (NTA) - " + title;
                String summaryHi = "राष्ट्रीय परीक्षा एजेंसी द्वारा आधिकारिक सूचना: " + title;
                String contentHash = PoliteHttpClient.computeSha256(title + ":" + officialUrl);

                ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                        examId,
                        SOURCE_ID,
                        noticeType,
                        title,
                        summary,
                        officialUrl,
                        "https://exams.nta.ac.in",
                        NoticeStatus.PUBLISHED,
                        VerificationStatus.VERIFIED,
                        pubDate,
                        dates,
                        title,
                        contentHash,
                        "10+2 / Senior Secondary or Graduate depending on specific entrance test regulations.",
                        0.95
                );

                notices.add(noticeDto);
            } catch (Exception e) {
                log.warn("Error parsing individual NTA row: {}", e.getMessage());
            }
        }

        return notices;
    }

    private String mapExamId(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("jee") || lower.contains("joint entrance")) {
            return "exam-nta-jee-main";
        } else if (lower.contains("neet") || lower.contains("medical")) {
            return "exam-nta-neet-ug";
        } else if (lower.contains("cuet") || lower.contains("common university")) {
            return "exam-nta-cuet-ug";
        }
        return "exam-nta-jee-main";
    }

    private NoticeType detectNoticeType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("admit card") || lower.contains("hall ticket") || lower.contains("city intimation")) {
            return NoticeType.ADMIT_CARD;
        } else if (lower.contains("result") || lower.contains("score card") || lower.contains("cutoff") || lower.contains("rank")) {
            return NoticeType.RESULT;
        } else if (lower.contains("answer key") || lower.contains("recorded response") || lower.contains("challenge")) {
            return NoticeType.ANSWER_KEY;
        } else if (lower.contains("corrigendum") || lower.contains("extension") || lower.contains("rescheduled")) {
            return NoticeType.CORRIGENDUM;
        }
        return NoticeType.RECRUITMENT;
    }
}
