package com.examradar.backend.repository;

import com.examradar.backend.domain.Source;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SourceRepository extends JpaRepository<Source, String> {
    List<Source> findByIsEnabledTrue();
}
