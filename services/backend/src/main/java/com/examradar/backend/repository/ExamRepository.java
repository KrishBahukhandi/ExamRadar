package com.examradar.backend.repository;

import com.examradar.backend.domain.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamRepository extends JpaRepository<Exam, String> {
    List<Exam> findByCategoryId(String categoryId);
    List<Exam> findByCategoryIdAndIsActiveTrue(String categoryId);
    List<Exam> findByIsActiveTrue();
    List<Exam> findByOrganizationIgnoreCase(String organization);
}
