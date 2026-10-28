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
            eligibilitySummary = "Bachelor's degree in any discipline. Age 21-32 years as of August 1, 2025 with standard reservation relaxations.",
            totalVacancies = 1056,
            vacancyBreakdownJson = "{\"UR\": 420, \"OBC\": 285, \"EWS\": 105, \"SC\": 160, \"ST\": 86, \"PwBD\": 40}",
            payScaleSummary = "Pay Level 10 (₹56,100 – ₹1,77,500) Group 'A'",
            applicationFeeGeneral = 100,
            applicationFeeReserved = 0,
            feeExemptionDetails = "₹100 for Gen/OBC/EWS Male candidates. ₹0 (Exempted) for Female, SC, ST, and PwBD candidates.",
            ageMin = 21,
            ageMax = 32,
            ageCutoffDate = LocalDate.of(2025, 8, 1),
            ageRelaxationSummary = "SC/ST: +5 yrs (Max 37), OBC: +3 yrs (Max 35), PwBD: +10 yrs (Max 42), Defence Disabled: +3 yrs",
            educationalQualification = "Degree from a recognized University or equivalent. Final year students eligible to apply subject to producing proof of passing before Civil Services (Mains)."
        ),
        NoticeModel(
            id = "fixture-notice-2",
            examId = "exam-ssc-cgl",
            examName = "Combined Graduate Level 2025",
            organization = "SSC",
            noticeType = NoticeType.RECRUITMENT,
            title = "[DEMO FIXTURE] SSC CGL 2025 Official Recruitment Notice — 17,727 Group B & C Posts",
            summary = "Staff Selection Commission invites online applications for Inspectors, Sub-Inspectors, Auditors, and Assistants across central government ministries.",
            officialNoticeUrl = "https://ssc.gov.in",
            officialApplyUrl = "https://ssc.gov.in/portal/apply",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.now().minusDays(10),
            applicationDeadline = LocalDate.now().plusDays(15),
            examDate = LocalDate.now().plusMonths(2),
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis() - 7200000L,
            isDemonstrationFixture = true,
            importantDates = listOf(
                ImportantDateModel(DateType.APPLICATION_START, LocalDate.now().minusDays(10)),
                ImportantDateModel(DateType.APPLICATION_DEADLINE, LocalDate.now().plusDays(15)),
                ImportantDateModel(DateType.CORRECTION_WINDOW, LocalDate.now().plusDays(18)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusMonths(2))
            ),
            eligibilitySummary = "Bachelor's Degree from a recognized University. Age 18-30/32 years depending on post.",
            totalVacancies = 17727,
            vacancyBreakdownJson = "{\"UR\": 7291, \"OBC\": 4586, \"EWS\": 1772, \"SC\": 2650, \"ST\": 1428}",
            payScaleSummary = "Pay Level 4 to Level 8 (₹25,500 – ₹1,51,100)",
            applicationFeeGeneral = 100,
            applicationFeeReserved = 0,
            feeExemptionDetails = "₹100 for Gen/OBC Male. Women candidates and SC/ST/PwD/ESM eligible for 100% fee waiver.",
            ageMin = 18,
            ageMax = 30,
            ageCutoffDate = LocalDate.of(2025, 8, 1),
            ageRelaxationSummary = "OBC: +3 yrs (Max 33), SC/ST: +5 yrs (Max 35), PwD (UR): +10 yrs, PwD (OBC): +13 yrs, PwD (SC/ST): +15 yrs",
            educationalQualification = "Bachelor's Degree from a recognized University or Institute. Specific posts (Junior Statistical Officer) require Statistics/Maths at degree or 10+2 level."
        ),
        NoticeModel(
            id = "fixture-notice-3",
            examId = "exam-ibps-po",
            examName = "IBPS CRP PO/MT XV",
            organization = "IBPS",
            noticeType = NoticeType.RECRUITMENT,
            title = "[DEMO FIXTURE] IBPS PO XV Official Notification — 4,455 Probationary Officer Vacancies",
            summary = "Common Recruitment Process for Probationary Officers and Management Trainees in 11 participating public sector banks.",
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
                ImportantDateModel(DateType.APPLICATION_START, LocalDate.now().minusDays(14)),
                ImportantDateModel(DateType.APPLICATION_DEADLINE, LocalDate.now().plusDays(7)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusMonths(2))
            ),
            eligibilitySummary = "Degree (Graduation) in any discipline from a Govt. of India recognized University.",
            totalVacancies = 4455,
            vacancyBreakdownJson = "{\"UR\": 1824, \"OBC\": 1198, \"EWS\": 445, \"SC\": 670, \"ST\": 318}",
            payScaleSummary = "Starting Basic Pay ₹48,480/month (Scale I Officer)",
            applicationFeeGeneral = 850,
            applicationFeeReserved = 175,
            feeExemptionDetails = "₹175 (incl. GST) for SC/ST/PwBD candidates. ₹850 (incl. GST) for all other applicants.",
            ageMin = 20,
            ageMax = 30,
            ageCutoffDate = LocalDate.of(2025, 8, 1),
            ageRelaxationSummary = "SC/ST: +5 yrs (Max 35), OBC (Non-Creamy): +3 yrs (Max 33), PwBD: +10 yrs (Max 40), Ex-Servicemen: +5 yrs",
            educationalQualification = "A Degree (Graduation) in any discipline from a University recognized by the Govt. Of India. Candidate must possess valid Mark-sheet / Degree Certificate."
        ),
        NoticeModel(
            id = "fixture-notice-4",
            examId = "exam-rbi-grade-b",
            examName = "RBI Grade B Officers 2025",
            organization = "RBI",
            noticeType = NoticeType.RECRUITMENT,
            title = "[DEMO FIXTURE] RBI Grade B (General/DEPR/DSIM) 2025 Notification",
            summary = "Recruitment for Officers in Grade 'B' (Direct Recruit-DR) in Reserve Bank of India.",
            officialNoticeUrl = "https://opportunities.rbi.org.in",
            officialApplyUrl = "https://ibpsonline.ibps.in/rbi2025",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.now().minusDays(2),
            applicationDeadline = LocalDate.now().plusDays(20),
            examDate = LocalDate.now().plusDays(45),
            isTentativeDeadline = false,
            lastCheckedTimestamp = System.currentTimeMillis() - 3600000L,
            isDemonstrationFixture = true,
            importantDates = listOf(
                ImportantDateModel(DateType.APPLICATION_START, LocalDate.now().minusDays(2)),
                ImportantDateModel(DateType.APPLICATION_DEADLINE, LocalDate.now().plusDays(20)),
                ImportantDateModel(DateType.EXAM_DATE, LocalDate.now().plusDays(45), isTentative = true)
            ),
            eligibilitySummary = "Graduation with minimum 60% marks (50% for SC/ST/PwBD) or Post-Graduation with 55% marks.",
            totalVacancies = 291,
            vacancyBreakdownJson = "{\"UR\": 130, \"OBC\": 65, \"EWS\": 29, \"SC\": 42, \"ST\": 25}",
            payScaleSummary = "Starting Gross Emoluments ~₹1,16,684/month (Basic Pay ₹55,200)",
            applicationFeeGeneral = 850,
            applicationFeeReserved = 100,
            feeExemptionDetails = "₹100 (Intimation Charges only) for SC/ST/PwBD. ₹850 for Gen/OBC/EWS candidates.",
            ageMin = 21,
            ageMax = 30,
            ageCutoffDate = LocalDate.of(2025, 7, 1),
            ageRelaxationSummary = "SC/ST: +5 yrs, OBC: +3 yrs, M.Phil: +2 yrs (32 max), Ph.D.: +4 yrs (34 max), PwBD: +10 yrs",
            educationalQualification = "Minimum 60% marks (50% for SC/ST/PwBD) in Bachelor's degree or equivalent examination."
        )
    )
}
