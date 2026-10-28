package com.examradar.backend.repository;

import com.examradar.backend.domain.NotificationJob;
import com.examradar.backend.domain.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationJobRepository extends JpaRepository<NotificationJob, UUID> {
    Optional<NotificationJob> findByIdempotencyKey(String idempotencyKey);
    List<NotificationJob> findByIdempotencyKeyIn(Collection<String> idempotencyKeys);
    List<NotificationJob> findByStatusAndScheduledAtLessThanEqual(JobStatus status, OffsetDateTime time);
    List<NotificationJob> findByNoticeIdAndStatus(UUID noticeId, JobStatus status);
    List<NotificationJob> findTop50ByUserIdOrderByScheduledAtDesc(UUID userId);
}
