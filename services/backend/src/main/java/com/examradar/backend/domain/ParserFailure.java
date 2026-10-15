package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "parser_failures")
public class ParserFailure {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(name = "document_url", nullable = false, length = 1024)
    private String documentUrl;

    @Column(name = "parser_version", nullable = false, length = 32)
    private String parserVersion;

    @Column(name = "error_stack", nullable = false, columnDefinition = "TEXT")
    private String errorStack;

    @Column(name = "raw_snippet", columnDefinition = "TEXT")
    private String rawSnippet;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ParserFailure() {}

    public ParserFailure(Source source, String documentUrl, String parserVersion, String errorStack, String rawSnippet) {
        this.source = source;
        this.documentUrl = documentUrl;
        this.parserVersion = parserVersion;
        this.errorStack = errorStack;
        this.rawSnippet = rawSnippet;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    public String getErrorStack() { return errorStack; }
    public void setErrorStack(String errorStack) { this.errorStack = errorStack; }

    public String getRawSnippet() { return rawSnippet; }
    public void setRawSnippet(String rawSnippet) { this.rawSnippet = rawSnippet; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
