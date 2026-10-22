package com.examradar.backend.controller;

import com.examradar.backend.dto.ApiResponse;
import com.examradar.backend.dto.ExamCategoryDto;
import com.examradar.backend.dto.ExamDto;
import com.examradar.backend.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/exams")
@Tag(name = "Exams", description = "Public examination catalogue and categories")
public class ExamController {

    private final ExamService examService;

    public ExamController(ExamService examService) {
        this.examService = examService;
    }

    @GetMapping
    @Operation(summary = "Discover official government examinations with optional category and search filters")
    public ApiResponse<List<ExamDto>> getExams(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String search
    ) {
        return ApiResponse.ok(examService.getExams(categoryId, search));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get examination details by unique exam ID")
    public ApiResponse<ExamDto> getExamById(@PathVariable String id) {
        return ApiResponse.ok(examService.getExamById(id));
    }

    @GetMapping("/categories")
    @Operation(summary = "List all recruitment categories (UPSC, SSC, Banking, Regulatory, PSUs)")
    public ApiResponse<List<ExamCategoryDto>> getCategories() {
        return ApiResponse.ok(examService.getCategories());
    }
}
