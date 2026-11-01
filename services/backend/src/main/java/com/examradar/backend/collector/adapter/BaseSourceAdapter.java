package com.examradar.backend.collector.adapter;

import com.examradar.backend.collector.ExtractedNoticeDto;
import com.examradar.backend.collector.ExtractionResult;
import com.examradar.backend.collector.PoliteHttpClient;
import com.examradar.backend.collector.SourceAdapter;
import com.examradar.backend.domain.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base template adapter implementing DRY principle across all official source scrapers.
 * Encapsulates polite HTTP execution, latency tracking, status code validation,
 * SHA-256 change detection, and standardized exception handling.
 */
public abstract class BaseSourceAdapter implements SourceAdapter {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected final PoliteHttpClient httpClient;

    protected BaseSourceAdapter(PoliteHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Target official portal or API endpoint URL.
     */
    protected abstract String getTargetUrl();

    /**
     * Human-readable portal label for logs and diagnostics (e.g., "UPSC", "SSC", "IBPS").
     */
    protected abstract String getPortalName();

    /**
     * Parses the HTTP response body and returns candidate notices.
     * Can be HTML or JSON parsing.
     */
    protected abstract List<ExtractedNoticeDto> parseResponseBody(String body) throws Exception;

    @Override
    public ExtractionResult fetchAndExtract(Source source, String lastContentHash) {
        long startTime = System.currentTimeMillis();
        String sourceId = getSourceId();
        String portalName = getPortalName();

        try {
            PoliteHttpClient.HttpResponseData response = httpClient.get(getTargetUrl());
            long latency = response.responseTimeMs();

            if (response.statusCode() != 200) {
                return ExtractionResult.failure(sourceId, response.statusCode(), latency,
                        "HTTP error from " + portalName + " portal: " + response.statusCode());
            }

            // Change detection using SHA-256
            if (lastContentHash != null && lastContentHash.equals(response.contentHash())) {
                return ExtractionResult.noChange(sourceId, response.contentHash(), response.statusCode(), latency);
            }

            List<ExtractedNoticeDto> notices = parseResponseBody(response.body());

            if (notices.isEmpty()) {
                log.warn("{} parsed but 0 notices extracted. Portal layout or schema may have changed.", portalName);
            }

            return ExtractionResult.success(sourceId, true, response.contentHash(),
                    response.statusCode(), latency, notices);

        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            log.error("Exception fetching {} notices: {}", portalName, e.getMessage(), e);
            return ExtractionResult.failure(sourceId, 0, latency,
                    "Exception during " + portalName + " fetch: " + e.getMessage());
        }
    }
}
