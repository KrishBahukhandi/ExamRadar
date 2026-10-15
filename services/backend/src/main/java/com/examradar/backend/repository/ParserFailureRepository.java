package com.examradar.backend.repository;

import com.examradar.backend.domain.ParserFailure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParserFailureRepository extends JpaRepository<ParserFailure, UUID> {
    List<ParserFailure> findTop20ByOrderByCreatedAtDesc();
    List<ParserFailure> findBySourceIdOrderByCreatedAtDesc(String sourceId);
}
