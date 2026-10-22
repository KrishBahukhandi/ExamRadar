package com.examradar.backend.service;

import com.examradar.backend.domain.Exam;
import com.examradar.backend.domain.ExamCategory;
import com.examradar.backend.dto.ExamCategoryDto;
import com.examradar.backend.dto.ExamDto;
import com.examradar.backend.repository.ExamCategoryRepository;
import com.examradar.backend.repository.ExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamServiceTest {

    @Mock
    private ExamRepository examRepository;

    @Mock
    private ExamCategoryRepository categoryRepository;

    @InjectMocks
    private ExamService examService;

    private ExamCategory testCategory;
    private Exam testExam;

    @BeforeEach
    void setUp() {
        testCategory = new ExamCategory("cat-upsc", "UPSC", "upsc", "Civil Services", 1);
        testExam = new Exam("exam-upsc-cse", testCategory, "UPSC", "Civil Services Exam", "CSE", "https://upsc.gov.in", "IAS & IPS");
    }

    @Test
    void testGetExamsWithoutFilter() {
        when(examRepository.findAll()).thenReturn(List.of(testExam));

        List<ExamDto> result = examService.getExams(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).examCode()).isEqualTo("CSE");
        verify(examRepository, times(1)).findAll();
    }

    @Test
    void testGetExamsWithCategoryFilter() {
        when(examRepository.findByCategoryId("cat-upsc")).thenReturn(List.of(testExam));

        List<ExamDto> result = examService.getExams("cat-upsc", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo("cat-upsc");
        verify(examRepository, times(1)).findByCategoryId("cat-upsc");
    }

    @Test
    void testGetExamsWithSearchFilter() {
        when(examRepository.findAll()).thenReturn(List.of(testExam));

        List<ExamDto> matching = examService.getExams(null, "Civil");
        List<ExamDto> nonMatching = examService.getExams(null, "Banking");

        assertThat(matching).hasSize(1);
        assertThat(nonMatching).isEmpty();
    }

    @Test
    void testGetExamByIdFound() {
        when(examRepository.findById("exam-upsc-cse")).thenReturn(Optional.of(testExam));

        ExamDto result = examService.getExamById("exam-upsc-cse");

        assertThat(result.id()).isEqualTo("exam-upsc-cse");
        assertThat(result.organization()).isEqualTo("UPSC");
    }

    @Test
    void testGetExamByIdNotFound() {
        when(examRepository.findById("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.getExamById("invalid"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Examination not found: invalid");
    }

    @Test
    void testGetCategories() {
        when(categoryRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(List.of(testCategory));

        List<ExamCategoryDto> categories = examService.getCategories();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).slug()).isEqualTo("upsc");
    }
}
