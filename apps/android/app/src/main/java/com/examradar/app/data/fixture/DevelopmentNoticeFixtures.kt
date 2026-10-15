package com.examradar.app.data.fixture

import com.examradar.app.data.model.*
import java.time.LocalDate

/**
 * Clearly labelled development fixtures.
 * STRICT POLICY:
 * Demonstration fixtures are used exclusively in local development and offline preview modes.
 * They are explicitly marked with `isDemonstrationFixture = true` and display visual disclaimer banners.
 */
object DevelopmentNoticeFixtures {

    val categories = listOf(
        CategoryModel("cat-upsc", "UPSC", "upsc", "Civil Services, Engineering Services & Defence", 4),
        CategoryModel("cat-ssc", "SSC", "ssc", "Combined Graduate Level, CHSL, CPO & JE", 4),
        CategoryModel("cat-banking", "Banking & IBPS", "banking", "IBPS PO, Clerk, SO & State Bank of India", 5),
        CategoryModel("cat-regulatory", "Regulatory Bodies", "regulatory", "RBI Grade B, SEBI Grade A & NABARD", 3),
        CategoryModel("cat-psu", "PSUs & Technical", "psu", "GATE PSU Recruitment & ONGC Trainees", 2)
    )

    val exams = listOf(
        ExamModel(
            id = "exam-upsc-cse",
            categoryId = "cat-upsc",
            organization = "Union Public Service Commission",
            examName = "Civil Services Examination 2025",
            examCode = "UPSC CSE",
            officialPortalUrl = "https://upsc.gov.in/examinations/active-examinations",
            description = "Recruitment for Indian Administrative Service, Indian Police Service, and Group A/B central civil services.",
            isFollowing = true,
            activeNoticeCount = 2
        ),
        ExamModel(
            id = "exam-ssc-cgl",
            categoryId = "cat-ssc",
            organization = "Staff Selection Commission",
            examName = "Combined Graduate Level Examination 2025",
            examCode = "SSC CGL",
            officialPortalUrl = "https://ssc.gov.in",
            description = "Recruitment to Group B and C posts across various ministries and central government departments.",
            isFollowing = true,
            activeNoticeCount = 1
        ),
        ExamModel(
            id = "exam-ibps-po",
            categoryId = "cat-banking",
            organization = "Institute of Banking Personnel Selection",
            examName = "IBPS CRP PO/MT XV",
            examCode = "IBPS PO",
            officialPortalUrl = "https://www.ibps.in",
            description = "Common recruitment process for Probationary Officers across 11 participating public sector banks.",
            isFollowing = true,
            activeNoticeCount = 1
        ),
        ExamModel(
            id = "exam-rbi-grade-b",
            categoryId = "cat-regulatory",
            organization = "Reserve Bank of India",
            examName = "RBI Grade B Officers 2025",
            examCode = "RBI Gr B",
            officialPortalUrl = "https://opportunities.rbi.org.in",
            description = "Premier recruitment for Manager-level Grade B officers across General, DEPR and DSIM streams.",
            isFollowing = false,
            activeNoticeCount = 1
        ),
        ExamModel(
            id = "exam-sbi-po",
            categoryId = "cat-banking",
            organization = "State Bank of India",
            examName = "SBI Probationary Officer 2025",
            examCode = "SBI PO",
            officialPortalUrl = "https://sbi.co.in/web/careers",
            description = "Annual nationwide recruitment for Probationary Officers in State Bank of India.",
            isFollowing = false,
            activeNoticeCount = 1
        )
    )

    val sampleNotices = listOf(
        NoticeModel(
            id = "fixture-notice-1",
            examId = "exam-upsc-cse",
            examName = "Civil Services Examination 2025",
            organization = "UPSC",
            noticeType = NoticeType.RECRUITMENT,
            title = "[DEMO FIXTURE] UPSC CSE 2025 Official Notification & Online Application Window",
            summary = "Official recruitment notification inviting online applications for 1,056 vacancies across civil services.",
            officialNoticeUrl = "https://upsc.gov.in/examinations/active-examinations",
            officialApplyUrl = "https://upsconline.nic.in",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.now().minusDays(5),
            applicationDeadline = LocalDate.now().plusDays(3),
            examDate = LocalDate.now().plusMonths(3),
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis() - 3600000L,
            isDemonstrationFixture = true,
            importantDates = listOf(
                ImportantDateModel(DateType.APPLICATION_START, LocalDate.now().minusDays(5)),
                ImportantDateModel(DateType.APPLICATION_DEADLINE, LocalDate.now().plusDays(3)),
                ImportantDateModel(DateType.CORRECTION_WINDOW, LocalDate.now().plusDays(5)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusMonths(3))
            ),
            eligibilitySummary = "Bachelor's degree in any discipline. Age 21-32 years as of August 1, 2025 with standard reservation relaxations."
        ),
        NoticeModel(
            id = "fixture-notice-2",
            examId = "exam-ssc-cgl",
            examName = "Combined Graduate Level 2025",
            organization = "SSC",
            noticeType = NoticeType.ADMIT_CARD,
            title = "[DEMO FIXTURE] SSC CGL Tier-1 Examination City Intimation & Admit Card Slip",
            summary = "Candidates can download their examination admission certificate and view allotted test center city.",
            officialNoticeUrl = "https://ssc.gov.in",
            officialApplyUrl = null,
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.AUTOMATED_HIGH_CONFIDENCE,
            publishedDate = LocalDate.now().minusDays(1),
            applicationDeadline = null,
            examDate = LocalDate.now().plusDays(12),
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis() - 7200000L,
            isDemonstrationFixture = true,
            importantDates = listOf(
                ImportantDateModel(DateType.ADMIT_CARD_RELEASE, LocalDate.now().minusDays(1)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusDays(12))
            ),
            eligibilitySummary = "Graduate degree required. Check regional SSC website for shift allocation."
        ),
        NoticeModel(
            id = "fixture-notice-3",
            examId = "exam-ibps-po",
            examName = "IBPS CRP PO/MT XV",
            organization = "IBPS",
            noticeType = NoticeType.RECRUITMENT,
            title = "[DEMO FIXTURE] IBPS PO XV Application Deadline Approaching — Last 7 Days",
            summary = "Closing window for online application registration and online payment of fees for Probationary Officers.",
            officialNoticeUrl = "https://www.ibps.in",
            officialApplyUrl = "https://ibpsonline.ibps.in",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.now().minusDays(14),
            applicationDeadline = LocalDate.now().plusDays(7),
            examDate = LocalDate.now().plusMonths(2),
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis() - 1800000L,
            isDemonstrationFixture = true,
            importantDates = listOf(
                ImportantDateModel(DateType.APPLICATION_DEADLINE, LocalDate.now().plusDays(7)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusMonths(2))
            ),
            eligibilitySummary = "Degree (Graduation) in any discipline from a recognized University in India."
        ),
        NoticeModel(
            id = "fixture-notice-4",
            examId = "exam-rbi-grade-b",
            examName = "RBI Grade B Officers 2025",
            organization = "RBI",
            noticeType = NoticeType.RESULT,
            title = "[DEMO FIXTURE] RBI Grade B Phase-1 Official Result & Marksheet Release",
            summary = "Roll numbers of candidates shortlisted for Phase-II Examination and individual scorecard links.",
            officialNoticeUrl = "https://opportunities.rbi.org.in",
            officialApplyUrl = null,
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.now().minusDays(2),
            applicationDeadline = null,
            examDate = LocalDate.now().plusDays(25),
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis() - 3600000L,
            isDemonstrationFixture = true,
            importantDates = listOf(
                ImportantDateModel(DateType.RESULT_DATE, LocalDate.now().minusDays(2)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusDays(25), isTentative = true)
            ),
            eligibilitySummary = "Shortlisted candidates from Phase-1 examination."
        )
    )
}
