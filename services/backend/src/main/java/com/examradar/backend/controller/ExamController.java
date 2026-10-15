package com.examradar.backend.controller;

import com.examradar.backend.domain.Exam;
import com.examradar.backend.domain.ExamCategory;
import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.ExamCategoryDto;
import com.examradar.backend.dto.ExamDto;
import com.examradar.backend.repository.ExamCategoryRepository;
import com.examradar.backend.repository.ExamRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams", description = "Public examination catalogue and categories")
public class ExamController {

    private final ExamRepository examRepository;
    private final ExamCategoryRepository categoryRepository;

    public ExamController(ExamRepository examRepository, ExamCategoryRepository categoryRepository) {
        this.examRepository = examRepository;
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    @Operation(summary = "Discover official government examinations with optional category and search filters")
    public ApiResponse<List<ExamDto>> getExams(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String search
    ) {
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

        List<ExamDto> dtos = exams.stream().map(this::toDto).toList();
        return ApiResponse.ok(dtos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get examination details by unique exam ID")
    public ApiResponse<ExamDto> getExamById(@PathVariable String id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Examination not found: " + id));
        return ApiResponse.ok(toDto(exam));
    }

    @GetMapping("/categories")
    @Operation(summary = "List all recruitment categories (UPSC, SSC, Banking, Regulatory, PSUs)")
    public ApiResponse<List<ExamCategoryDto>> getCategories() {
        List<ExamCategoryDto> categories = categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(c -> new ExamCategoryDto(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getDisplayOrder()))
                .toList();
        return ApiResponse.ok(categories);
    }

    private ExamDto toDto(Exam e) {
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
