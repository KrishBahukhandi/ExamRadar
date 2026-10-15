package com.examradar.backend.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "profiles")
public class UserProfile {

    @Id
    private UUID id; // Maps directly to Supabase auth.users(id)

    @Column(name = "display_name", length = 128)
    private String displayName;

    @Column(name = "graduation_status", length = 32)
    private String graduationStatus;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "education_level", length = 64)
    private String educationLevel;

    @Column(name = "target_state", length = 64)
    private String targetState = "ALL_INDIA";

    @Column(name = "language_preference", length = 16)
    private String languagePreference = "en";

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public UserProfile() {}

    public UserProfile(UUID id, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.graduationStatus = "GRADUATED";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UserProfile(UUID id, String email, String displayName) {
        this.id = id;
        this.displayName = displayName;
        this.graduationStatus = "GRADUATED";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UserProfile(UUID id, String displayName, String graduationStatus, Integer graduationYear, String educationLevel) {
        this.id = id;
        this.displayName = displayName;
        this.graduationStatus = graduationStatus;
        this.graduationYear = graduationYear;
        this.educationLevel = educationLevel;
        this.targetState = "ALL_INDIA";
        this.languagePreference = "en";
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getGraduationStatus() { return graduationStatus; }
    public void setGraduationStatus(String graduationStatus) { this.graduationStatus = graduationStatus; }

    public Integer getGraduationYear() { return graduationYear; }
    public void setGraduationYear(Integer graduationYear) { this.graduationYear = graduationYear; }

    public String getEducationLevel() { return educationLevel; }
    public void setEducationLevel(String educationLevel) { this.educationLevel = educationLevel; }

    public String getTargetState() { return targetState; }
    public void setTargetState(String targetState) { this.targetState = targetState; }

    public String getLanguagePreference() { return languagePreference; }
    public void setLanguagePreference(String languagePreference) { this.languagePreference = languagePreference; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
