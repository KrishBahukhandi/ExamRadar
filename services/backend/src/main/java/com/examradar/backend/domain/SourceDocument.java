package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "source_documents")
public class SourceDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private SourceFetchRun run;

    @Column(name = "document_url", nullable = false, length = 1024)
    private String documentUrl;

    @Column(name = "document_type", nullable = false, length = 32)
    private String documentType; // HTML, PDF, IMAGE

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "raw_text_preview", columnDefinition = "TEXT")
    private String rawTextPreview;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public SourceDocument() {}

    public SourceDocument(Source source, SourceFetchRun run, String documentUrl, String documentType, String contentHash, String rawTextPreview) {
        this.source = source;
        this.run = run;
        this.documentUrl = documentUrl;
        this.documentType = documentType;
        this.contentHash = contentHash;
        this.rawTextPreview = rawTextPreview;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public SourceFetchRun getRun() { return run; }
    public void setRun(SourceFetchRun run) { this.run = run; }

    public String getDocumentUrl() { return documentUrl; }
    public void setDocumentUrl(String documentUrl) { this.documentUrl = documentUrl; }

    public String getDocumentType() { return documentType; }
    public void setDocumentType(String documentType) { this.documentType = documentType; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getRawTextPreview() { return rawTextPreview; }
    public void setRawTextPreview(String rawTextPreview) { this.rawTextPreview = rawTextPreview; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
