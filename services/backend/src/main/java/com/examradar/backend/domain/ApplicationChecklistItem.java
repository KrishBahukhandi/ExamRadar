package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "application_checklist_items", uniqueConstraints = {
    @UniqueConstraint(name = "uq_checklist_item", columnNames = {"checklist_id", "item_key"})
})
public class ApplicationChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checklist_id", nullable = false)
    private ApplicationChecklist checklist;

    @Column(name = "item_key", nullable = false, length = 64)
    private String itemKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ApplicationChecklistItem() {}

    public ApplicationChecklistItem(ApplicationChecklist checklist, String itemKey, String title) {
        this.checklist = checklist;
        this.itemKey = itemKey;
        this.title = title;
        this.isCompleted = false;
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public ApplicationChecklist getChecklist() { return checklist; }
    public void setChecklist(ApplicationChecklist checklist) { this.checklist = checklist; }

    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Boolean getIsCompleted() { return isCompleted; }
    public void setIsCompleted(Boolean completed) {
        isCompleted = completed;
        completedAt = completed ? OffsetDateTime.now() : null;
    }

    public OffsetDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(OffsetDateTime completedAt) { this.completedAt = completedAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
