package com.examradar.app

import com.examradar.app.data.model.DateType
import com.examradar.app.data.model.ImportantDateModel
import com.examradar.app.data.model.NoticeModel
import com.examradar.app.data.model.NoticeType
import com.examradar.app.data.model.VerificationStatus
import com.examradar.app.notification.DeadlineAlarmScheduler
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class DeadlineAlarmSchedulerTest {

    private val zoneIst = DeadlineAlarmScheduler.ZONE_IST

    @Test
    fun testComputeTriggerEpochMillisT48h() {
        val deadline = LocalDate.of(2026, 11, 20)
        val cutoffInstant = deadline.atTime(17, 0).atZone(zoneIst)
        val cutoffMillis = cutoffInstant.toInstant().toEpochMilli()

        val trigger48h = DeadlineAlarmScheduler.computeTriggerEpochMillis(
            deadlineDate = deadline,
            hoursBefore = 48,
            cutoffHour = 17,
            cutoffMinute = 0,
            zoneId = zoneIst
        )

        val diffMillis = cutoffMillis - trigger48h
        val expectedDiffMillis = 48L * 60 * 60 * 1000L
        assertEquals("T-48h trigger must be exactly 48 hours prior to cutoff", expectedDiffMillis, diffMillis)

        val triggerZonedDateTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(trigger48h),
            zoneIst
        )
        assertEquals(2026, triggerZonedDateTime.year)
        assertEquals(11, triggerZonedDateTime.monthValue)
        assertEquals(18, triggerZonedDateTime.dayOfMonth)
        assertEquals(17, triggerZonedDateTime.hour)
        assertEquals(0, triggerZonedDateTime.minute)
    }

    @Test
    fun testComputeTriggerEpochMillisT24h() {
        val deadline = LocalDate.of(2026, 11, 20)
        val cutoffInstant = deadline.atTime(17, 0).atZone(zoneIst)
        val cutoffMillis = cutoffInstant.toInstant().toEpochMilli()

        val trigger24h = DeadlineAlarmScheduler.computeTriggerEpochMillis(
            deadlineDate = deadline,
            hoursBefore = 24,
            cutoffHour = 17,
            cutoffMinute = 0,
            zoneId = zoneIst
        )

        val diffMillis = cutoffMillis - trigger24h
        val expectedDiffMillis = 24L * 60 * 60 * 1000L
        assertEquals("T-24h trigger must be exactly 24 hours prior to cutoff", expectedDiffMillis, diffMillis)

        val triggerZonedDateTime = ZonedDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(trigger24h),
            zoneIst
        )
        assertEquals(2026, triggerZonedDateTime.year)
        assertEquals(11, triggerZonedDateTime.monthValue)
        assertEquals(19, triggerZonedDateTime.dayOfMonth)
        assertEquals(17, triggerZonedDateTime.hour)
        assertEquals(0, triggerZonedDateTime.minute)
    }

    @Test
    fun testDeterministicRequestCodes() {
        val noticeId1 = "upsc-cse-2026"
        val noticeId2 = "ssc-cgl-2026"

        val req48Notice1 = DeadlineAlarmScheduler.getRequestCode(noticeId1, 48)
        val req24Notice1 = DeadlineAlarmScheduler.getRequestCode(noticeId1, 24)
        val req48Notice2 = DeadlineAlarmScheduler.getRequestCode(noticeId2, 48)

        // Must be non-negative
        assertTrue("Request codes must be non-negative positive integers", req48Notice1 >= 0)
        assertTrue("Request codes must be non-negative positive integers", req24Notice1 >= 0)
        assertTrue("Request codes must be non-negative positive integers", req48Notice2 >= 0)

        // Must be distinct for different intervals on the same notice
        assertNotEquals("48h and 24h request codes must be distinct to prevent collision", req48Notice1, req24Notice1)

        // Must be distinct for different notices
        assertNotEquals("Different notices must produce distinct request codes", req48Notice1, req48Notice2)

        // Must be strictly reproducible (idempotent)
        assertEquals(req48Notice1, DeadlineAlarmScheduler.getRequestCode(noticeId1, 48))
    }

    @Test
    fun testShouldScheduleTriggerBoundary() {
        val triggerEpoch = 1770000000000L

        // Current time is before trigger -> should schedule
        assertTrue(DeadlineAlarmScheduler.shouldScheduleTrigger(triggerEpoch, triggerEpoch - 10000L))

        // Current time is equal to or after trigger -> should not schedule
        assertFalse(DeadlineAlarmScheduler.shouldScheduleTrigger(triggerEpoch, triggerEpoch))
        assertFalse(DeadlineAlarmScheduler.shouldScheduleTrigger(triggerEpoch, triggerEpoch + 5000L))
    }

    @Test
    fun testResolveDeadlineDateHierarchy() {
        // Case 1: applicationDeadline explicitly defined
        val noticeWithAppDeadline = createMockNotice(
            appDeadline = LocalDate.of(2026, 12, 1),
            feeDeadline = LocalDate.of(2026, 12, 3)
        )
        assertEquals(
            LocalDate.of(2026, 12, 1),
            DeadlineAlarmScheduler.resolveDeadlineDate(noticeWithAppDeadline)
        )

        // Case 2: applicationDeadline null, fallback to feePaymentDeadline
        val noticeWithFeeDeadline = createMockNotice(
            appDeadline = null,
            feeDeadline = LocalDate.of(2026, 12, 3)
        )
        assertEquals(
            LocalDate.of(2026, 12, 3),
            DeadlineAlarmScheduler.resolveDeadlineDate(noticeWithFeeDeadline)
        )

        // Case 3: applicationDeadline and feePaymentDeadline null, fallback to importantDates
        val noticeWithImportantDates = createMockNotice(
            appDeadline = null,
            feeDeadline = null,
            importantDates = listOf(
                ImportantDateModel(
                    dateType = DateType.APPLICATION_DEADLINE,
                    eventDate = LocalDate.of(2026, 12, 5)
                )
            )
        )
        assertEquals(
            LocalDate.of(2026, 12, 5),
            DeadlineAlarmScheduler.resolveDeadlineDate(noticeWithImportantDates)
        )

        // Case 4: No deadlines present -> returns null
        val noticeWithoutDeadline = createMockNotice(
            appDeadline = null,
            feeDeadline = null,
            importantDates = emptyList()
        )
        assertNull(DeadlineAlarmScheduler.resolveDeadlineDate(noticeWithoutDeadline))
    }

    private fun createMockNotice(
        appDeadline: LocalDate?,
        feeDeadline: LocalDate?,
        importantDates: List<ImportantDateModel> = emptyList()
    ): NoticeModel {
        return NoticeModel(
            id = "test-notice-id",
            examId = "exam-test",
            examName = "Test Examination",
            organization = "Test Commission",
            noticeType = NoticeType.RECRUITMENT,
            title = "Test Recruitment Notice",
            summary = "Summary",
            officialNoticeUrl = "https://example.com/notice.pdf",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            applicationDeadline = appDeadline,
            feePaymentDeadline = feeDeadline,
            importantDates = importantDates
        )
    }
}
