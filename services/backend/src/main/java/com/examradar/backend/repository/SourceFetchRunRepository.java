package com.examradar.backend.repository;

import com.examradar.backend.domain.SourceFetchRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SourceFetchRunRepository extends JpaRepository<SourceFetchRun, UUID> {
    List<SourceFetchRun> findTop10BySourceIdOrderByCreatedAtDesc(String sourceId);
}
