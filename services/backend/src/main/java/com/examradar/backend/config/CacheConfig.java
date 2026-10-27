package com.examradar.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise in-memory caching configuration powered by Caffeine.
 * Provides high-throughput, sub-millisecond latency caching for static and semi-static
 * public catalog entities (Exams, Categories, Sources) to eliminate redundant
 * database queries on high-frequency mobile app cold-starts.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String CACHE_EXAM_CATEGORIES = "examCategories";
    public static final String CACHE_EXAMS = "exams";
    public static final String CACHE_EXAM_BY_ID = "examById";
    public static final String CACHE_SOURCES = "sources";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();
        List<CaffeineCache> caches = List.of(
                buildCache(CACHE_EXAM_CATEGORIES, 60, TimeUnit.MINUTES, 100),
                buildCache(CACHE_EXAMS, 30, TimeUnit.MINUTES, 1000),
                buildCache(CACHE_EXAM_BY_ID, 30, TimeUnit.MINUTES, 500),
                buildCache(CACHE_SOURCES, 30, TimeUnit.MINUTES, 100)
        );
        cacheManager.setCaches(caches);
        return cacheManager;
    }

    private CaffeineCache buildCache(String name, long duration, TimeUnit unit, long maxSize) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .expireAfterWrite(duration, unit)
                .maximumSize(maxSize)
                .recordStats()
                .build());
    }
}
