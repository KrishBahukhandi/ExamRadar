package com.examradar.backend.repository;

import com.examradar.backend.domain.NoticeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoticeVersionRepository extends JpaRepository<NoticeVersion, UUID> {
    List<NoticeVersion> findByNoticeIdOrderByVersionNumberDesc(UUID noticeId);
    Optional<NoticeVersion> findByNoticeIdAndVersionNumber(UUID noticeId, Integer versionNumber);
}
