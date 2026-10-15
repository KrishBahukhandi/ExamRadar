package com.examradar.backend.repository;

import com.examradar.backend.domain.ApplicationChecklistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationChecklistItemRepository extends JpaRepository<ApplicationChecklistItem, UUID> {
    List<ApplicationChecklistItem> findByChecklistId(UUID checklistId);
    Optional<ApplicationChecklistItem> findByChecklistIdAndItemKey(UUID checklistId, String itemKey);
}
