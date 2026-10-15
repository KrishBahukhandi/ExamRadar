package com.examradar.backend.collector;

import com.examradar.backend.domain.Source;

public interface SourceAdapter {
    /**
     * Unique source ID matching registry in sources table (e.g. 'src-ibps', 'src-ssc').
     */
    String getSourceId();

    /**
     * Executes polite HTTP request, content hashing, HTML/PDF extraction, and returns candidate notices.
     */
    ExtractionResult fetchAndExtract(Source source, String lastContentHash);
}
