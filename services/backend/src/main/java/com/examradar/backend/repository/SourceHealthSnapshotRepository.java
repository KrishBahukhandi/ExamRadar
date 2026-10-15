package com.examradar.backend.repository;

import com.examradar.backend.domain.SourceHealthSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceHealthSnapshotRepository extends JpaRepository<SourceHealthSnapshot, UUID> {
    Optional<SourceHealthSnapshot> findTopBySourceIdOrderBySnapshotTimestampDesc(String sourceId);
    List<SourceHealthSnapshot> findTop10ByOrderBySnapshotTimestampDesc();
}
