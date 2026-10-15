package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "source_health_snapshots")
public class SourceHealthSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_id", nullable = false)
    private Source source;

    @Column(name = "total_runs", nullable = false)
    private Integer totalRuns = 0;

    @Column(name = "successful_runs", nullable = false)
    private Integer successfulRuns = 0;

    @Column(name = "failed_runs", nullable = false)
    private Integer failedRuns = 0;

    @Column(name = "average_latency_ms", nullable = false)
    private Long averageLatencyMs = 0L;

    @Column(name = "quarantined_notices_count", nullable = false)
    private Integer quarantinedNoticesCount = 0;

    @Column(name = "snapshot_timestamp", nullable = false)
    private OffsetDateTime snapshotTimestamp = OffsetDateTime.now();

    public SourceHealthSnapshot() {}

    public SourceHealthSnapshot(Source source, Integer totalRuns, Integer successfulRuns,
                                Integer failedRuns, Long averageLatencyMs, Integer quarantinedNoticesCount) {
        this.source = source;
        this.totalRuns = totalRuns;
        this.successfulRuns = successfulRuns;
        this.failedRuns = failedRuns;
        this.averageLatencyMs = averageLatencyMs;
        this.quarantinedNoticesCount = quarantinedNoticesCount;
        this.snapshotTimestamp = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }

    public Integer getTotalRuns() { return totalRuns; }
    public void setTotalRuns(Integer totalRuns) { this.totalRuns = totalRuns; }

    public Integer getSuccessfulRuns() { return successfulRuns; }
    public void setSuccessfulRuns(Integer successfulRuns) { this.successfulRuns = successfulRuns; }

    public Integer getFailedRuns() { return failedRuns; }
    public void setFailedRuns(Integer failedRuns) { this.failedRuns = failedRuns; }

    public Long getAverageLatencyMs() { return averageLatencyMs; }
    public void setAverageLatencyMs(Long averageLatencyMs) { this.averageLatencyMs = averageLatencyMs; }

    public Integer getQuarantinedNoticesCount() { return quarantinedNoticesCount; }
    public void setQuarantinedNoticesCount(Integer quarantinedNoticesCount) { this.quarantinedNoticesCount = quarantinedNoticesCount; }

    public OffsetDateTime getSnapshotTimestamp() { return snapshotTimestamp; }
    public void setSnapshotTimestamp(OffsetDateTime snapshotTimestamp) { this.snapshotTimestamp = snapshotTimestamp; }
}
