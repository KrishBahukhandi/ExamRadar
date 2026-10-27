package com.examradar.backend.cache;

import com.examradar.backend.config.CacheConfig;
import com.examradar.backend.domain.Exam;
import com.examradar.backend.domain.ExamCategory;
import com.examradar.backend.domain.Source;
import com.examradar.backend.dto.ExamCategoryDto;
import com.examradar.backend.dto.ExamDto;
import com.examradar.backend.dto.SourceHealthDto;
import com.examradar.backend.repository.ExamCategoryRepository;
import com.examradar.backend.repository.ExamRepository;
import com.examradar.backend.repository.SourceRepository;
import com.examradar.backend.service.AdminService;
import com.examradar.backend.service.ExamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CatalogCacheIntegrationTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private ExamCategoryRepository categoryRepository;

    @Autowired
    private SourceRepository sourceRepository;

    @BeforeEach
    void setUp() {
        examService.evictCatalogCaches();
        Cache sourceCache = cacheManager.getCache(CacheConfig.CACHE_SOURCES);
        if (sourceCache != null) {
            sourceCache.clear();
        }

        ExamCategory cat = new ExamCategory("cat-cache-test", "Cache Category", "cache-cat", "Desc", 99);
        categoryRepository.save(cat);

        Exam exam = new Exam("exam-cache-1", cat, "TEST_ORG", "Cache Examination", "CACHE_EXAM", "https://example.gov.in", "desc");
        examRepository.save(exam);

        Source source = new Source(
                "src-cache-test", "Cache Source Org", "Cache Source",
                "https://example.gov.in/notices",
                com.examradar.backend.domain.enums.SourceType.HTML_SCRAPE,
                com.examradar.backend.domain.enums.FetchMethod.HTTP_GET,
                60, "1.0", "Notes"
        );
        sourceRepository.save(source);
    }

    @Test
    void testExamCategoriesCached() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_EXAM_CATEGORIES);
        assertThat(cache).isNotNull();

        // First call populates cache
        List<ExamCategoryDto> categories1 = examService.getCategories();
        assertThat(categories1).isNotEmpty();

        // Verify cache entry exists
        assertThat(cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY)).isNotNull();

        // Second call should return the exact same data from cache
        List<ExamCategoryDto> categories2 = examService.getCategories();
        assertThat(categories2).hasSize(categories1.size());
    }

    @Test
    void testExamByIdCachedAndEvicted() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_EXAM_BY_ID);
        assertThat(cache).isNotNull();

        ExamDto exam1 = examService.getExamById("exam-cache-1");
        assertThat(exam1).isNotNull();
        assertThat(cache.get("exam-cache-1")).isNotNull();

        // Evict caches
        examService.evictCatalogCaches();
        assertThat(cache.get("exam-cache-1")).isNull();
    }

    @Test
    void testExamsListCached() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_EXAMS);
        assertThat(cache).isNotNull();

        List<ExamDto> list1 = examService.getExams("cat-cache-test", null);
        assertThat(list1).isNotEmpty();

        // Cache key format: 'c:cat-cache-test:s:all'
        assertThat(cache.get("c:cat-cache-test:s:all")).isNotNull();

        List<ExamDto> list2 = examService.getExams("cat-cache-test", null);
        assertThat(list2).hasSize(list1.size());
    }

    @Test
    void testAdminSourcesCached() {
        Cache cache = cacheManager.getCache(CacheConfig.CACHE_SOURCES);
        assertThat(cache).isNotNull();

        List<SourceHealthDto> sources = adminService.getSources();
        assertThat(sources).isNotEmpty();

        assertThat(cache.get(org.springframework.cache.interceptor.SimpleKey.EMPTY)).isNotNull();
    }
}
