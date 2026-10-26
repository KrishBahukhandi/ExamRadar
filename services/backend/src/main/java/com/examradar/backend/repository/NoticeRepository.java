package com.examradar.backend.repository;

import com.examradar.backend.domain.Notice;
import com.examradar.backend.domain.enums.NoticeStatus;
import com.examradar.backend.domain.enums.NoticeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, UUID> {
    List<Notice> findByStatusOrderByPublishedDateDesc(NoticeStatus status);
    List<Notice> findByStatus(NoticeStatus status);
    List<Notice> findByExamId(String examId);
    List<Notice> findByExamIdAndStatus(String examId, NoticeStatus status);
    List<Notice> findByStatusAndVerificationStatus(NoticeStatus status, com.examradar.backend.domain.enums.VerificationStatus verificationStatus);
    Optional<Notice> findByOfficialNoticeUrl(String officialNoticeUrl);

    @Query("SELECT n FROM Notice n WHERE n.status = 'PUBLISHED' " +
           "AND (:examId IS NULL OR n.exam.id = :examId) " +
           "AND (:noticeType IS NULL OR n.noticeType = :noticeType) " +
           "ORDER BY n.publishedDate DESC")
    Page<Notice> findPublishedNotices(
            @Param("examId") String examId,
            @Param("noticeType") NoticeType noticeType,
            Pageable pageable
    );

    @Query("SELECT n FROM Notice n WHERE (:status IS NULL OR n.status = :status) " +
           "AND (:examId IS NULL OR n.exam.id = :examId) " +
           "AND (:noticeType IS NULL OR n.noticeType = :noticeType) " +
           "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR (n.summary IS NOT NULL AND LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY n.publishedDate DESC")
    List<Notice> findFiltered(
            @Param("examId") String examId,
            @Param("noticeType") NoticeType noticeType,
            @Param("status") NoticeStatus status,
            @Param("search") String search
    );

    @Query("SELECT n FROM Notice n WHERE (:status IS NULL OR n.status = :status) " +
           "AND (:examId IS NULL OR n.exam.id = :examId) " +
           "AND (:noticeType IS NULL OR n.noticeType = :noticeType) " +
           "AND (:search IS NULL OR LOWER(n.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR (n.summary IS NOT NULL AND LOWER(n.summary) LIKE LOWER(CONCAT('%', :search, '%')))) " +
           "ORDER BY n.publishedDate DESC")
    Page<Notice> findFiltered(
            @Param("examId") String examId,
            @Param("noticeType") NoticeType noticeType,
            @Param("status") NoticeStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
