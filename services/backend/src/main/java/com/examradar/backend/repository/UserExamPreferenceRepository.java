package com.examradar.backend.repository;

import com.examradar.backend.domain.UserExamPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserExamPreferenceRepository extends JpaRepository<UserExamPreference, UUID> {
    List<UserExamPreference> findByUserId(UUID userId);
    Optional<UserExamPreference> findByUserIdAndExamId(UUID userId, String examId);
    List<UserExamPreference> findByExamIdAndNotifyNewNoticesTrue(String examId);
    List<UserExamPreference> findByExamIdAndNotifyDeadlinesTrue(String examId);
}
