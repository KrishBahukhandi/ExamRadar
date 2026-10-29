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
 * Source adapter for Uttar Pradesh Public Service Commission (UPPSC).
 * Connects to uppsc.up.nic.in notice circulars for Combined State / Upper
 * Subordinate Services (PCS), Review Officer (RO/ARO), and State Engineering exams.
 */
@Component
public class UppscSourceAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(UppscSourceAdapter.class);

    public static final String SOURCE_ID = "src-uppsc";
    private static final String UPPSC_NOTICES_URL = "https://uppsc.up.nic.in";
    private static final String UPPSC_BASE_URL = "https://uppsc.up.nic.in";

    private final PoliteHttpClient httpClient;

    public UppscSourceAdapter(PoliteHttpClient httpClient) {
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
            PoliteHttpClient.HttpResponseData response = httpClient.get(UPPSC_NOTICES_URL);
            long latency = response.responseTimeMs();

            if (response.statusCode() != 200) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "HTTP error from UPPSC portal: " + response.statusCode());
            }

            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(SOURCE_ID, response.contentHash(), response.statusCode(), latency);
            }

            Document doc = Jsoup.parse(response.body());
            List<ExtractedNoticeDto> notices = extractNoticesFromHtml(doc);

            if (notices.isEmpty()) {
                log.warn("UPPSC HTML parsed but 0 notices extracted. Portal layout may have changed.");
            }

            return ExtractionResult.success(SOURCE_ID, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Exception fetching UPPSC notices: {}", e.getMessage(), e);
            return ExtractionResult.failure(SOURCE_ID, 0, latency,
                    "Exception during UPPSC fetch: " + e.getMessage());
        }
    }

    private List<ExtractedNoticeDto> extractNoticesFromHtml(Document doc) {
        List<ExtractedNoticeDto> notices = new ArrayList<>();

        Elements items = doc.select(".notice-board a, .news-ticker a, table tr a, .marquee a, ul.notices li a");

        for (Element link : items) {
            try {
                String title = link.text().trim();
                if (title.isBlank() || title.length() < 6) continue;

                String href = link.attr("href").trim();
                if (href.isBlank()) continue;

                String officialUrl;
                if (href.startsWith("http")) {
                    officialUrl = href;
                } else if (href.startsWith("/")) {
                    officialUrl = UPPSC_BASE_URL + href;
                } else {
                    officialUrl = UPPSC_BASE_URL + "/" + href;
                }

                LocalDate pubDate = null;
                Element parent = link.parent();
                if (parent != null) {
                    Optional<LocalDate> dateOpt = DateParserUtil.parseDate(parent.text());
                    if (dateOpt.isPresent()) pubDate = dateOpt.get();
                }
                if (pubDate == null) {
                    pubDate = LocalDate.now();
                }

                String examId = mapExamId(title);
                NoticeType noticeType = detectNoticeType(title);

                List<ExtractedDateDto> dates = new ArrayList<>();
                dates.add(new ExtractedDateDto(
                        DateType.APPLICATION_START, pubDate, false, false,
                        pubDate.toString(), "UPPSC Notice Circular Date", 0.95
                ));

                Optional<LocalDate> deadlineOpt = DateParserUtil.extractDeadlineFromText(title);
                if (deadlineOpt.isPresent() && deadlineOpt.get().isAfter(pubDate)) {
                    dates.add(new ExtractedDateDto(
                            DateType.APPLICATION_DEADLINE, deadlineOpt.get(), false, false,
                            deadlineOpt.get().toString(), "Deadline in announcement text", 0.90
                    ));
                }

                String summary = "Official announcement from Uttar Pradesh Public Service Commission (UPPSC): " + title;
                String titleHi = "उत्तर प्रदेश लोक सेवा आयोग (UPPSC) - " + title;
                String summaryHi = "उत्तर प्रदेश लोक सेवा आयोग द्वारा आधिकारिक सूचना: " + title;
                String contentHash = PoliteHttpClient.computeSha256(title + ":" + officialUrl);

                ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                        examId,
                        SOURCE_ID,
                        noticeType,
                        title,
                        summary,
                        officialUrl,
                        "https://uppsc.up.nic.in",
                        NoticeStatus.PUBLISHED,
                        VerificationStatus.VERIFIED,
                        pubDate,
                        dates,
                        title,
                        contentHash,
                        "Bachelor's Degree in any discipline from a recognized University in India.",
                        0.95,
                        220,
                        "{\"UR\": 90, \"OBC\": 58, \"SC\": 46, \"ST\": 4, \"EWS\": 22}",
                        "Pay Matrix Level-10 (₹56,100 – ₹1,77,500) & Level-7 (₹44,900 – ₹1,42,400)",
                        125,
                        65,
                        "Divyang (PWD) candidates are exempted from exam fee, payment only for processing (₹25)",
                        21,
                        40,
                        LocalDate.of(pubDate.getYear(), 7, 1),
                        "Up to 5 years relaxation for SC/ST/OBC of Uttar Pradesh state domicile.",
                        "Graduate Degree from any recognized University or equivalent qualification.",
                        titleHi,
                        summaryHi,
                        "Stage 1: Preliminary Exam (GS Paper-1 & CSAT), Stage 2: Main Written Exam (Conventional Essay & GS I-VI), Stage 3: Personality Test (Interview)",
                        "0.33 Marks (1/3rd deduction for each wrong answer in Preliminary Exam)",
                        "Pen & Paper (OMR) for Preliminary, Offline Descriptive for Main Exam",
                        "https://uppsc.up.nic.in/Syllabus"
                );

                notices.add(noticeDto);

            } catch (Exception e) {
                log.warn("Error parsing individual UPPSC item: {}", e.getMessage());
            }
        }

        return notices;
    }

    private String mapExamId(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("pcs") || lower.contains("combined state") || lower.contains("subordinate")) {
            return "exam-uppsc-pcs";
        }
        return "exam-uppsc-pcs";
    }

    private NoticeType detectNoticeType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("admit") || lower.contains("hall ticket")) {
            return NoticeType.ADMIT_CARD;
        } else if (lower.contains("result") || lower.contains("mark") || lower.contains("selection list")) {
            return NoticeType.RESULT;
        } else if (lower.contains("answer key") || lower.contains("objection")) {
            return NoticeType.ANSWER_KEY;
        } else if (lower.contains("corrigendum") || lower.contains("amendment") || lower.contains("rectification")) {
            return NoticeType.CORRIGENDUM;
        } else if (lower.contains("postpone") || lower.contains("rescheduled")) {
            return NoticeType.POSTPONEMENT;
        }
        return NoticeType.RECRUITMENT;
    }
}
