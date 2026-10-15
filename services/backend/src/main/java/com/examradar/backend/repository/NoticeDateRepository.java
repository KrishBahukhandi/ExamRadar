package com.examradar.backend.repository;

import com.examradar.backend.domain.NoticeDate;
import com.examradar.backend.domain.enums.DateType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface NoticeDateRepository extends JpaRepository<NoticeDate, UUID> {
    List<NoticeDate> findByNoticeId(UUID noticeId);
    List<NoticeDate> findByNoticeIdOrderByEventDateAsc(UUID noticeId);
    List<NoticeDate> findByEventDateBetweenOrderByEventDateAsc(LocalDate startDate, LocalDate endDate);

    @Query("SELECT nd FROM NoticeDate nd WHERE nd.notice.status = 'PUBLISHED' " +
           "AND nd.eventDate >= :startDate AND nd.eventDate <= :endDate " +
           "ORDER BY nd.eventDate ASC")
    List<NoticeDate> findCalendarEvents(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Query("SELECT nd FROM NoticeDate nd WHERE nd.notice.status = 'PUBLISHED' " +
           "AND nd.dateType = 'APPLICATION_DEADLINE' " +
           "AND nd.eventDate >= :today AND nd.eventDate <= :deadlineWindow " +
           "ORDER BY nd.eventDate ASC")
    List<NoticeDate> findApproachingDeadlines(
            @Param("today") LocalDate today,
            @Param("deadlineWindow") LocalDate deadlineWindow
    );
}
