package com.examradar.backend.service;

import com.examradar.backend.domain.Exam;
import com.examradar.backend.dto.ExamCategoryDto;
import com.examradar.backend.dto.ExamDto;
import com.examradar.backend.repository.ExamCategoryRepository;
import com.examradar.backend.repository.ExamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Transactional(readOnly = true)
public class ExamService {

    private final ExamRepository examRepository;
    private final ExamCategoryRepository categoryRepository;

    public ExamService(ExamRepository examRepository, ExamCategoryRepository categoryRepository) {
        this.examRepository = examRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ExamDto> getExams(String categoryId, String search) {
        List<Exam> exams;
        if (categoryId != null && !categoryId.isBlank()) {
            exams = examRepository.findByCategoryId(categoryId);
        } else {
            exams = examRepository.findAll();
        }

        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase();
            exams = exams.stream()
                    .filter(e -> e.getExamName().toLowerCase().contains(q) ||
                                 e.getExamCode().toLowerCase().contains(q) ||
                                 e.getOrganization().toLowerCase().contains(q))
                    .toList();
        }

        return exams.stream().map(this::toDto).toList();
    }

    public ExamDto getExamById(String id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Examination not found: " + id));
        return toDto(exam);
    }

    public List<ExamCategoryDto> getCategories() {
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new ExamCategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getDisplayOrder()))
                .toList();
    }

    public ExamDto toDto(Exam e) {
        return new ExamDto(
                e.getId(),
                e.getCategory() != null ? e.getCategory().getId() : null,
                e.getCategory() != null ? e.getCategory().getName() : null,
                e.getOrganization(),
                e.getExamName(),
                e.getExamCode(),
                e.getOfficialPortalUrl(),
                e.getDescription(),
                e.getIsActive()
        );
    }
}
