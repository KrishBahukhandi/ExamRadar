package com.examradar.backend.domain;

import com.examradar.backend.domain.enums.FetchMethod;
import com.examradar.backend.domain.enums.SourceType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "sources")
public class Source {

    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, length = 128)
    private String organization;

    @Column(name = "source_name", nullable = false, length = 255)
    private String sourceName;

    @Column(name = "official_base_url", nullable = false, length = 512)
    private String officialBaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 32)
    private SourceType sourceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "fetch_method", nullable = false, length = 32)
    private FetchMethod fetchMethod;

    @Column(name = "polling_interval_minutes")
    private Integer pollingIntervalMinutes = 60;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    @Column(name = "parser_version", nullable = false, length = 32)
    private String parserVersion;

    @Column(name = "last_attempted_fetch")
    private OffsetDateTime lastAttemptedFetch;

    @Column(name = "last_successful_fetch")
    private OffsetDateTime lastSuccessfulFetch;

    @Column(name = "last_successful_content_hash", length = 64)
    private String lastSuccessfulContentHash;

    @Column(name = "consecutive_failures", nullable = false)
    private Integer consecutiveFailures = 0;

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "access_policy_notes", columnDefinition = "TEXT")
    private String accessPolicyNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Source() {}

    public Source(String id, String organization, String sourceName, String officialBaseUrl,
                  SourceType sourceType, FetchMethod fetchMethod, Integer pollingIntervalMinutes,
                  String parserVersion, String accessPolicyNotes) {
        this.id = id;
        this.organization = organization;
        this.sourceName = sourceName;
        this.officialBaseUrl = officialBaseUrl;
        this.sourceType = sourceType;
        this.fetchMethod = fetchMethod;
        this.pollingIntervalMinutes = pollingIntervalMinutes;
        this.parserVersion = parserVersion;
        this.accessPolicyNotes = accessPolicyNotes;
        this.isEnabled = true;
        this.consecutiveFailures = 0;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getOrganization() { return organization; }
    public void setOrganization(String organization) { this.organization = organization; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }

    public String getOfficialBaseUrl() { return officialBaseUrl; }
    public void setOfficialBaseUrl(String officialBaseUrl) { this.officialBaseUrl = officialBaseUrl; }

    public SourceType getSourceType() { return sourceType; }
    public void setSourceType(SourceType sourceType) { this.sourceType = sourceType; }

    public FetchMethod getFetchMethod() { return fetchMethod; }
    public void setFetchMethod(FetchMethod fetchMethod) { this.fetchMethod = fetchMethod; }

    public Integer getPollingIntervalMinutes() { return pollingIntervalMinutes; }
    public void setPollingIntervalMinutes(Integer pollingIntervalMinutes) { this.pollingIntervalMinutes = pollingIntervalMinutes; }

    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean enabled) { isEnabled = enabled; }

    public String getParserVersion() { return parserVersion; }
    public void setParserVersion(String parserVersion) { this.parserVersion = parserVersion; }

    public OffsetDateTime getLastAttemptedFetch() { return lastAttemptedFetch; }
    public void setLastAttemptedFetch(OffsetDateTime lastAttemptedFetch) { this.lastAttemptedFetch = lastAttemptedFetch; }

    public OffsetDateTime getLastSuccessfulFetch() { return lastSuccessfulFetch; }
    public void setLastSuccessfulFetch(OffsetDateTime lastSuccessfulFetch) { this.lastSuccessfulFetch = lastSuccessfulFetch; }

    public String getLastSuccessfulContentHash() { return lastSuccessfulContentHash; }
    public void setLastSuccessfulContentHash(String lastSuccessfulContentHash) { this.lastSuccessfulContentHash = lastSuccessfulContentHash; }

    public Integer getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(Integer consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }

    public String getAccessPolicyNotes() { return accessPolicyNotes; }
    public void setAccessPolicyNotes(String accessPolicyNotes) { this.accessPolicyNotes = accessPolicyNotes; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
