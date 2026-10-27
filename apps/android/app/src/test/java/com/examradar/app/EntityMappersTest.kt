package com.examradar.app

import com.examradar.app.data.local.*
import com.examradar.app.data.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class EntityMappersTest {

    @Test
    fun categoryModelToEntityAndBack() {
        val model = CategoryModel(
            id = "cat-upsc",
            name = "UPSC",
            slug = "upsc",
            description = "Civil Services and Central Services",
            examCount = 12
        )

        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.name, entity.name)
        assertEquals(model.examCount, entity.examCount)

        val mappedBack = entity.toDomainModel()
        assertEquals(model, mappedBack)
    }

    @Test
    fun examModelToEntityAndBack() {
        val model = ExamModel(
            id = "exam-upsc-cse",
            categoryId = "cat-upsc",
            organization = "UPSC",
            examName = "Civil Services Examination",
            examCode = "CSE",
            officialPortalUrl = "https://upsc.gov.in",
            description = "Premier recruitment for IAS, IPS, and IFS",
            isFollowing = true,
            activeNoticeCount = 4
        )

        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.examCode, entity.examCode)
        assertEquals(model.isFollowing, entity.isFollowing)

        val mappedBack = entity.toDomainModel()
        assertEquals(model, mappedBack)
    }

    @Test
    fun noticeModelToEntityAndBack() {
        val model = NoticeModel(
            id = "notif-upsc-2026-001",
            examId = "exam-upsc-cse",
            examName = "Civil Services Examination 2026",
            organization = "UPSC",
            noticeType = NoticeType.RECRUITMENT,
            title = "Civil Services (Preliminary) Examination, 2026",
            summary = "Official recruitment notification announced for 1,056 vacancies.",
            officialNoticeUrl = "https://upsc.gov.in/sites/default/files/Notice-CSPE-2026.pdf",
            officialApplyUrl = "https://upsconline.nic.in",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.of(2026, 2, 14),
            applicationDeadline = LocalDate.of(2026, 3, 5),
            examDate = LocalDate.of(2026, 5, 24),
            isTentativeDeadline = false,
            lastCheckedTimestamp = 1771000000000L,
            isDemonstrationFixture = false,
            importantDates = listOf(
                ImportantDateModel(
                    dateType = DateType.APPLICATION_START,
                    eventDate = LocalDate.of(2026, 2, 14),
                    isTentative = false,
                    isRevised = false
                ),
                ImportantDateModel(
                    dateType = DateType.APPLICATION_DEADLINE,
                    eventDate = LocalDate.of(2026, 3, 5),
                    isTentative = false,
                    isRevised = false
                ),
                ImportantDateModel(
                    dateType = DateType.EXAM_DATE,
                    eventDate = LocalDate.of(2026, 5, 24),
                    isTentative = false,
                    isRevised = false
                )
            ),
            eligibilitySummary = "Bachelor's degree from recognized university"
        )

        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.examId, entity.examId)
        assertEquals(model.noticeType, entity.noticeType)
        assertEquals(model.publishedDate, entity.publishedDate)

        val mappedBack = entity.toDomainModel()
        assertEquals(model.id, mappedBack.id)
        assertEquals(model.examId, mappedBack.examId)
        assertEquals(model.noticeType, mappedBack.noticeType)
        assertEquals(model.verificationStatus, mappedBack.verificationStatus)
        assertEquals(model.publishedDate, mappedBack.publishedDate)
        assertEquals(model.applicationDeadline, mappedBack.applicationDeadline)
        assertEquals(model.examDate, mappedBack.examDate)
        assertEquals(model.importantDates.size, mappedBack.importantDates.size)
        assertEquals(model.importantDates[0].eventDate, mappedBack.importantDates[0].eventDate)
        assertEquals(model.importantDates[1].dateType, mappedBack.importantDates[1].dateType)
    }

    @Test
    fun convertersHandleNullAndInvalidValuesGracefully() {
        val converters = Converters()

        assertNull(converters.fromLocalDate(null))
        assertNull(converters.toLocalDate(null))
        assertNull(converters.toLocalDate("invalid-date-string"))
        assertEquals(LocalDate.of(2026, 5, 24), converters.toLocalDate("2026-05-24"))

        assertNull(converters.fromNoticeType(null))
        assertNull(converters.toNoticeType(null))
        assertEquals(NoticeType.RECRUITMENT, converters.toNoticeType("INVALID_ENUM"))
        assertEquals(NoticeType.RESULT, converters.toNoticeType("RESULT"))

        assertNull(converters.fromVerificationStatus(null))
        assertNull(converters.toVerificationStatus(null))
        assertEquals(VerificationStatus.PENDING, converters.toVerificationStatus("NON_EXISTENT"))
        assertEquals(VerificationStatus.VERIFIED, converters.toVerificationStatus("VERIFIED"))
    }
}
