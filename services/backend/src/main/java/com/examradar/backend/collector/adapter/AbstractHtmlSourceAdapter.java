package com.examradar.backend.collector.adapter;

import com.examradar.backend.collector.ExtractedNoticeDto;
import com.examradar.backend.collector.PoliteHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;

/**
 * Base template adapter for HTML-based official exam portals (UPSC, NTA, RRB, UPPSC, BPSC).
 * Bridges HTML string to Jsoup Document parsing and delegates extraction to subclass.
 */
public abstract class AbstractHtmlSourceAdapter extends BaseSourceAdapter {

    protected AbstractHtmlSourceAdapter(PoliteHttpClient httpClient) {
        super(httpClient);
    }

    @Override
    protected List<ExtractedNoticeDto> parseResponseBody(String body) {
        Document doc = Jsoup.parse(body);
        return extractNoticesFromHtml(doc);
    }

    /**
     * Portal-specific HTML parsing logic extracting candidate notices from Jsoup Document.
     */
    protected abstract List<ExtractedNoticeDto> extractNoticesFromHtml(Document doc);
}
