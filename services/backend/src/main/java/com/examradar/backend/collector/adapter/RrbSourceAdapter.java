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
 * Adapter for Indian Railways Centralized Employment Notices (CEN) across Railway Recruitment Boards (RRBs).
 * Connects to official recruitment feeds for RRB NTPC, ALP, Group D, and technical cadres.
 */
@Component
public class RrbSourceAdapter extends AbstractHtmlSourceAdapter {

    public static final String SOURCE_ID = "src-rrb";
    private static final String RRB_NOTICES_URL = "https://indianrailways.gov.in";
    private static final String RRB_BASE_URL = "https://indianrailways.gov.in";

    public RrbSourceAdapter(PoliteHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    public String getSourceId() {
        return SOURCE_ID;
    }

    @Override
    protected String getTargetUrl() {
        return RRB_NOTICES_URL;
    }

    @Override
    protected String getPortalName() {
        return "RRB";
    }

    @Override
    protected List<ExtractedNoticeDto> extractNoticesFromHtml(Document doc) {
        List<ExtractedNoticeDto> notices = new ArrayList<>();

        Elements rows = doc.select("table tr, .whats-new li, .news-list li, .marquee-list li, a[href*='CEN'], a[href*='recruitment']");

        for (Element row : rows) {
            try {
                Element linkElement = row.tagName().equals("a") ? row : row.selectFirst("a[href]");
                if (linkElement == null) continue;

                String title = linkElement.text().trim();
                if (title.isBlank() || title.length() < 6) continue;

                String href = linkElement.attr("href").trim();
                String officialUrl;
                if (href.startsWith("http://") || href.startsWith("https://")) {
                    officialUrl = href;
                } else if (href.startsWith("/")) {
                    officialUrl = RRB_BASE_URL + href;
                } else {
                    officialUrl = RRB_BASE_URL + "/" + href;
                }

                LocalDate pubDate = DateParserUtil.parseDate(row.text()).orElse(LocalDate.now());

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

                String summary = "Official Centralized Employment Notice (CEN) from Indian Railways: " + title;
                String titleHi = "भारतीय रेलवे भर्ती बोर्ड (RRB) - " + title;
                String summaryHi = "रेलवे भर्ती बोर्ड द्वारा आधिकारिक रोजगार सूचना: " + title;
                String contentHash = PoliteHttpClient.computeSha256(title + ":" + officialUrl);

                ExtractedNoticeDto noticeDto = new ExtractedNoticeDto(
                        examId,
                        SOURCE_ID,
                        noticeType,
                        title,
                        summary,
                        officialUrl,
                        "https://indianrailways.gov.in",
                        NoticeStatus.PUBLISHED,
                        VerificationStatus.VERIFIED,
                        pubDate,
                        dates,
                        title,
                        contentHash,
                        "10th Pass / ITI / 12th / Graduate depending on CEN category specification.",
                        0.95
                );

                notices.add(noticeDto);
            } catch (Exception e) {
                log.warn("Error parsing individual RRB row: {}", e.getMessage());
            }
        }

        return notices;
    }

    private String mapExamId(String title) {
        String lower = title.toLowerCase();
        if (lower.contains("alp") || lower.contains("loco pilot") || lower.contains("technician")) {
            return "exam-rrb-alp";
        } else if (lower.contains("group d") || lower.contains("track maintainer") || lower.contains("level 1")) {
            return "exam-rrb-group-d";
        }
        return "exam-rrb-ntpc";
    }

    private NoticeType detectNoticeType(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("e-call letter") || lower.contains("admit card") || lower.contains("city slip") || lower.contains("hall ticket")) {
            return NoticeType.ADMIT_CARD;
        } else if (lower.contains("result") || lower.contains("scorecard") || lower.contains("cbt-1 result") || lower.contains("cbt-2 result")) {
            return NoticeType.RESULT;
        } else if (lower.contains("answer key") || lower.contains("objection tracker")) {
            return NoticeType.ANSWER_KEY;
        } else if (lower.contains("corrigendum") || lower.contains("amendment") || lower.contains("revised vacancy")) {
            return NoticeType.CORRIGENDUM;
        }
        return NoticeType.RECRUITMENT;
    }
}
