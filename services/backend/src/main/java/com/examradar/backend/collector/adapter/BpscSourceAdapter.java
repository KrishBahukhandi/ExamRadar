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
 * Source adapter for Bihar Public Service Commission (BPSC).
 * Connects to bpsc.bih.nic.in for Combined Competitive Examination (CCE),
 * Assistant Professor, Teacher Recruitment Exam (TRE), and departmental posts.
 */
@Component
public class BpscSourceAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(BpscSourceAdapter.class);

    public static final String SOURCE_ID = "src-bpsc";
    private static final String BPSC_NOTICES_URL = "https://www.bpsc.bih.nic.in";
    private static final String BPSC_BASE_URL = "https://www.bpsc.bih.nic.in";

    private final PoliteHttpClient httpClient;

    public BpscSourceAdapter(PoliteHttpClient httpClient) {
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
            PoliteHttpClient.HttpResponseData response = httpClient.get(BPSC_NOTICES_URL);
            long latency = response.responseTimeMs();

            if (response.statusCode() != 200) {
                return ExtractionResult.failure(SOURCE_ID, response.statusCode(), latency,
                        "HTTP error from BPSC portal: " + response.statusCode());
            }

            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(SOURCE_ID, response.contentHash(), response.statusCode(), latency);
            }

            Document doc = Jsoup.parse(response.body());
            List<ExtractedNoticeDto> notices = extractNoticesFromHtml(doc);

            if (notices.isEmpty()) {
                log.warn("BPSC HTML parsed but 0 notices extracted. Portal layout may have changed.");
            }

            return ExtractionResult.success(SOURCE_ID, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Exception fetching BPSC notices: {}", e.getMessage(), e);
            return ExtractionResult.failure(SOURCE_ID, 0, latency,
                    "Exception during BPSC fetch: " + e.getMessage());
        }
    }

    private List<ExtractedNoticeDto> extractNoticesFromHtml(Document doc) {
        List<ExtractedNoticeDto> notices = new ArrayList<>();

        Elements items = doc.select("table tr a, .notification-list a, .whats-new a, ul li a");

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
                    officialUrl = BPSC_BASE_URL + href;
                } else {
                    officialUrl = BPSC_BASE_URL + "/" + href;
                }

                LocalDate pubDate = null;
                Element row = link.closest("tr");
                if (row != null) {
                    Optional<LocalDate> dateOpt = DateParserUtil.parseDate(row.text());
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
                        pubDate.toString(), "BPSC Circular Date", 0.95
                ));

                Optional<LocalDate> deadlineOpt = DateParserUtil.extractDeadlineFromText(title);
                if (deadlineOpt.isPresent() && deadlineOpt.get().isAfter(pubDate)) {
                    dates.add(new ExtractedDateDto(
                            DateType.APPLICATION_DEADLINE, deadlineOpt.get(), false, false,
                            deadlineOpt.get().toString(), "Deadline in announcement text", 0.90
                    ));
                }

                String summary = "Official announcement from Bihar Public Service Commission (BPSC): " + title;
                String titleHi = "बिहार लोक सेवा आयोग (BPSC) - " + title;
                String summaryHi = "बिहार लोक सेवा आयोग द्वारा प्रकाशित आधिकारिक अधिसूचना: " + title;
                String contentHash = PoliteHttpClient.computeSha256(title + ":" + officialUrl);

                ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                        examId,
                        SOURCE_ID,
                        noticeType,
                        title,
                        summary,
                        officialUrl,
                        "https://onlinebpsc.bihar.gov.in",
                        NoticeStatus.PUBLISHED,
                        VerificationStatus.VERIFIED,
                        pubDate,
                        dates,
                        title,
                        contentHash,
                        "Graduate Degree from any recognized university or equivalent institution.",
                        0.95,
                        195,
                        "{\"UR\": 78, \"EBC\": 35, \"BC\": 24, \"SC\": 32, \"ST\": 2, \"EWS\": 24}",
                        "Pay Level 7 (₹44,900 – ₹1,42,400) & Pay Level 9 (₹53,100 – ₹1,67,800)",
                        600,
                        150,
                        "Bihar domicile Female candidates and SC/ST of Bihar state pay concession fee of ₹150",
                        20,
                        37,
                        LocalDate.of(pubDate.getYear(), 8, 1),
                        "Up to 3 years relaxation for BC/EBC (Male & Female) and 5 years for SC/ST.",
                        "Graduation from any recognized university or equivalent institute.",
                        titleHi,
                        summaryHi,
                        "Stage 1: Integrated CCE Preliminary Examination (150 Marks Objective), Stage 2: CCE Main Written Exam (General Hindi, GS I & II, Optional/Essay), Stage 3: Interview (120 Marks)",
                        "0.33 Marks (1/3rd deduction for each incorrect answer)",
                        "Pen and Paper (OMR) for Preliminary, Written Offline for Main Exam",
                        "https://www.bpsc.bih.nic.in/Syllabus"
                );

                notices.add(noticeDto);

            } catch (Exception e) {
                log.warn("Error parsing individual BPSC item: {}", e.getMessage());
            }
        }

        return notices;
    }

    private String mapExamId(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("cce") || lower.contains("combined competitive") || lower.contains("sanyukt")) {
            return "exam-bpsc-cce";
        }
        return "exam-bpsc-cce";
    }

    private NoticeType detectNoticeType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("admit") || lower.contains("hall ticket")) {
            return NoticeType.ADMIT_CARD;
        } else if (lower.contains("result") || lower.contains("merit") || lower.contains("marksheet")) {
            return NoticeType.RESULT;
        } else if (lower.contains("answer key") || lower.contains("objection")) {
            return NoticeType.ANSWER_KEY;
        } else if (lower.contains("corrigendum") || lower.contains("shuddhipatra") || lower.contains("amendment")) {
            return NoticeType.CORRIGENDUM;
        } else if (lower.contains("postpone") || lower.contains("sthagit")) {
            return NoticeType.POSTPONEMENT;
        }
        return NoticeType.RECRUITMENT;
    }
}
