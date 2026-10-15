package com.examradar.backend.repository;

import com.examradar.backend.domain.SourceDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SourceDocumentRepository extends JpaRepository<SourceDocument, UUID> {
    Optional<SourceDocument> findByContentHash(String contentHash);
    Optional<SourceDocument> findTopBySourceIdOrderByCreatedAtDesc(String sourceId);
}
