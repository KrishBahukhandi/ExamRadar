package com.examradar.app

import com.examradar.app.data.model.*
import com.examradar.app.util.ShareHelper
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class ShareHelperTest {

    @Test
    fun testFormatNoticeShareCardComplete() {
        val notice = NoticeModel(
            id = "upsc-cse-2026",
            examId = "exam-upsc-cse",
            examName = "Civil Services Examination 2026",
            title = "Civil Services (Preliminary) Examination, 2026",
            organization = "Union Public Service Commission",
            noticeType = NoticeType.RECRUITMENT,
            summary = "Official recruitment for IAS, IPS, and IFS cadres.",
            officialNoticeUrl = "https://upsc.gov.in/sites/default/files/Notice-CSPE-2026.pdf",
            officialApplyUrl = "https://upsconline.nic.in",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.of(2026, 2, 1),
            applicationDeadline = LocalDate.of(2026, 2, 24),
            examDate = LocalDate.of(2026, 5, 24),
            totalVacancies = 1056,
            payScaleSummary = "Pay Level 10 (₹56,100 - ₹1,77,500)",
            educationalQualification = "Graduate degree from a recognised university",
            ageMin = 21,
            ageMax = 32,
            applicationFeeGeneral = 100,
            applicationFeeReserved = 0,
            importantDates = listOf(
                ImportantDateModel(
                    dateType = DateType.APPLICATION_DEADLINE,
                    eventDate = LocalDate.of(2026, 2, 24),
                    isTentative = false,
                    isRevised = false
                )
            )
        )

        val card = ShareHelper.formatNoticeShareCard(notice)

        assertTrue(card.contains("OFFICIAL NOTICE: Civil Services (Preliminary) Examination, 2026"))
        assertTrue(card.contains("Authority:* Union Public Service Commission"))
        assertTrue(card.contains("Total Vacancies:* 1,056 Posts"))
        assertTrue(card.contains("Pay Scale:* Pay Level 10 (₹56,100 - ₹1,77,500)"))
        assertTrue(card.contains("Last Date to Apply:* 24 Feb 2026"))
        assertTrue(card.contains("Examination Date:* 24 May 2026"))
        assertTrue(card.contains("Qualification:* Graduate degree from a recognised university"))
        assertTrue(card.contains("Age Criteria:* 21 – 32 Years"))
        assertTrue(card.contains("Application Fee:* ₹100 (Reserved: Free (Exempt))"))
        assertTrue(card.contains("Apply Online Portal:*"))
        assertTrue(card.contains("https://upsconline.nic.in"))
        assertTrue(card.contains("Official Gazette PDF:*"))
        assertTrue(card.contains("https://upsc.gov.in/sites/default/files/Notice-CSPE-2026.pdf"))
        assertTrue(card.contains("100% Official & Verified Source"))
        assertTrue(card.contains("https://app.examradar.in/notices/upsc-cse-2026"))
    }

    @Test
    fun testFormatUrgentDeadlineShareCard() {
        val notice = NoticeModel(
            id = "ssc-cgl-2026",
            examId = "exam-ssc-cgl",
            examName = "Combined Graduate Level Examination 2026",
            title = "Notice for Combined Graduate Level Examination 2026",
            organization = "Staff Selection Commission",
            noticeType = NoticeType.RECRUITMENT,
            summary = "Recruitment for Group B and C posts.",
            officialNoticeUrl = "https://ssc.gov.in/notice_cgl_2026.pdf",
            officialApplyUrl = "https://ssc.gov.in/apply",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.of(2026, 6, 1),
            applicationDeadline = LocalDate.of(2026, 7, 4),
            totalVacancies = 14200
        )

        val card24h = ShareHelper.formatUrgentDeadlineShareCard(notice, 1)
        assertTrue(card24h.contains("URGENT APPLICATION DEADLINE: FINAL 24 HOURS!"))
        assertTrue(card24h.contains("Target Exam:* Combined Graduate Level Examination 2026"))
        assertTrue(card24h.contains("Vacancies at Stake:* 14,200 Posts"))
        assertTrue(card24h.contains("https://app.examradar.in/notices/ssc-cgl-2026"))

        val card48h = ShareHelper.formatUrgentDeadlineShareCard(notice, 2)
        assertTrue(card48h.contains("URGENT APPLICATION DEADLINE: 2 DAYS LEFT!"))
    }

    @Test
    fun testFormatExamCountdownShareCard() {
        val card = ShareHelper.formatExamCountdownShareCard(
            examName = "UPSC CSE Prelims 2026",
            daysLeft = 184,
            examDateStr = "24 May 2026"
        )

        assertTrue(card.contains("EXAM RADAR COUNTDOWN: UPSC CSE Prelims 2026"))
        assertTrue(card.contains("Scheduled Date:* 24 May 2026"))
        assertTrue(card.contains("Days Remaining:* 184 Days Left"))
        assertTrue(card.contains("Focus on revision, previous year questions (PYQs), and mock tests"))
        assertTrue(card.contains("https://app.examradar.in"))
    }

    @Test
    fun testFormatCalendarEventShareCard() {
        val event = CalendarEventModel(
            id = "cal-1",
            examId = "exam-ibps-po",
            examName = "IBPS PO / MT XIV",
            organization = "Institute of Banking Personnel Selection",
            dateType = DateType.ADMIT_CARD_RELEASE,
            eventDate = LocalDate.of(2026, 10, 10),
            isTentative = true,
            isRevised = false,
            noticeId = "ibps-po-2026",
            officialNoticeUrl = "https://ibps.in"
        )

        val card = ShareHelper.formatCalendarEventShareCard(event)

        assertTrue(card.contains("EXAM TIMELINE UPDATE: IBPS PO / MT XIV"))
        assertTrue(card.contains("Authority:* Institute of Banking Personnel Selection"))
        assertTrue(card.contains("Admit Card Release [TENTATIVE]"))
        assertTrue(card.contains("10 Oct 2026"))
        assertTrue(card.contains("https://app.examradar.in/notices/ibps-po-2026"))
    }

    @Test
    fun testFormatChecklistProgressShareCard() {
        val card = ShareHelper.formatChecklistProgressShareCard(
            examName = "UPSC Civil Services 2026",
            completedCount = 7,
            totalCount = 10
        )

        assertTrue(card.contains("Preparation Milestone for UPSC Civil Services 2026"))
        assertTrue(card.contains("Completed:* 7 / 10 Official Topics (70% Done)"))
        assertTrue(card.contains("https://app.examradar.in"))
    }
}

