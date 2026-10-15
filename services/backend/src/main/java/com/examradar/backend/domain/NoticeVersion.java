package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notice_versions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_notice_version", columnNames = {"notice_id", "version_number"})
})
public class NoticeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private SourceDocument document;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(name = "change_summary", columnDefinition = "TEXT")
    private String changeSummary;

    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData; // JSON representation of extracted payload

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public NoticeVersion() {}

    public NoticeVersion(Notice notice, Integer versionNumber, SourceDocument document,
                         String contentHash, String changeSummary, String extractedData) {
        this.notice = notice;
        this.versionNumber = versionNumber;
        this.document = document;
        this.contentHash = contentHash;
        this.changeSummary = changeSummary;
        this.extractedData = extractedData;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Notice getNotice() { return notice; }
    public void setNotice(Notice notice) { this.notice = notice; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public SourceDocument getDocument() { return document; }
    public void setDocument(SourceDocument document) { this.document = document; }

    public String getContentHash() { return contentHash; }
    public void setContentHash(String contentHash) { this.contentHash = contentHash; }

    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }

    public String getExtractedData() { return extractedData; }
    public void setExtractedData(String extractedData) { this.extractedData = extractedData; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
