package com.examradar.backend.repository;

import com.examradar.backend.domain.ApplicationChecklist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationChecklistRepository extends JpaRepository<ApplicationChecklist, UUID> {
    List<ApplicationChecklist> findByUserId(UUID userId);
    Optional<ApplicationChecklist> findByUserIdAndExamId(UUID userId, String examId);
}
