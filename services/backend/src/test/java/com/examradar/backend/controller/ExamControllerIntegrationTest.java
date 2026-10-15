package com.examradar.backend.controller;

import com.examradar.backend.domain.Exam;
import com.examradar.backend.domain.ExamCategory;
import com.examradar.backend.repository.ExamCategoryRepository;
import com.examradar.backend.repository.ExamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ExamControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamCategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        ExamCategory cat = new ExamCategory("cat-test-ctrl", "Test Category", "test-cat", "Desc", 1);
        categoryRepository.save(cat);

        Exam exam1 = new Exam("exam-test-1", cat, "SSC", "Combined Graduate Level", "CGL", "https://ssc.gov.in", "desc");
        Exam exam2 = new Exam("exam-test-2", cat, "IBPS", "IBPS PO CRP", "PO", "https://ibps.in", "desc");
        examRepository.save(exam1);
        examRepository.save(exam2);
    }

    @Test
    void testGetExamsPublicEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/exams")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void testGetExamsFilterBySearch() throws Exception {
        mockMvc.perform(get("/api/v1/exams")
                        .param("search", "Graduate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].examCode").value("CGL"));
    }

    @Test
    void testGetExamById() throws Exception {
        mockMvc.perform(get("/api/v1/exams/exam-test-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("exam-test-1"))
                .andExpect(jsonPath("$.data.organization").value("SSC"));
    }

    @Test
    void testGetCategories() throws Exception {
        mockMvc.perform(get("/api/v1/exams/categories")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}
