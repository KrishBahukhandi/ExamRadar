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
            eligibilitySummary = "Bachelor's degree from recognized university",
            totalVacancies = 1056,
            vacancyBreakdownJson = "{\"UR\": 420, \"OBC\": 285}",
            payScaleSummary = "Pay Level 10 (₹56,100 – ₹1,77,500)",
            applicationFeeGeneral = 100,
            applicationFeeReserved = 0,
            feeExemptionDetails = "Female/SC/ST exempted",
            ageMin = 21,
            ageMax = 32,
            ageCutoffDate = LocalDate.of(2026, 8, 1),
            ageRelaxationSummary = "+5 yrs SC/ST, +3 yrs OBC",
            educationalQualification = "Degree from recognized University",
            hasCorrigendum = true,
            corrigendumSummary = "Corrigendum #1: Schedule revised",
            photoSpecifications = "20-50KB white background",
            signatureSpecifications = "10-20KB black ink",
            feePaymentDeadline = LocalDate.of(2026, 3, 6),
            titleHi = "सिविल सेवा परीक्षा 2026",
            summaryHi = "1,056 पदों के लिए अधिसूचना",
            examStages = "Prelims, Mains, Interview",
            negativeMarking = "0.33 Marks",
            examMode = "Pen & Paper OMR",
            syllabusUrl = "https://upsc.gov.in/syllabus"
        )

        val entity = model.toEntity()
        assertEquals(model.id, entity.id)
        assertEquals(model.examId, entity.examId)
        assertEquals(model.noticeType, entity.noticeType)
        assertEquals(model.publishedDate, entity.publishedDate)
        assertEquals(1056, entity.totalVacancies)
        assertEquals("Pay Level 10 (₹56,100 – ₹1,77,500)", entity.payScaleSummary)
        assertEquals(100, entity.applicationFeeGeneral)
        assertEquals(0, entity.applicationFeeReserved)
        assertEquals(21, entity.ageMin)
        assertEquals(32, entity.ageMax)
        assertEquals(LocalDate.of(2026, 8, 1), entity.ageCutoffDate)
        assertEquals(true, entity.hasCorrigendum)
        assertEquals("Corrigendum #1: Schedule revised", entity.corrigendumSummary)
        assertEquals("20-50KB white background", entity.photoSpecifications)
        assertEquals("10-20KB black ink", entity.signatureSpecifications)
        assertEquals(LocalDate.of(2026, 3, 6), entity.feePaymentDeadline)
        assertEquals("सिविल सेवा परीक्षा 2026", entity.titleHi)
        assertEquals("1,056 पदों के लिए अधिसूचना", entity.summaryHi)
        assertEquals("Prelims, Mains, Interview", entity.examStages)
        assertEquals("0.33 Marks", entity.negativeMarking)
        assertEquals("Pen & Paper OMR", entity.examMode)
        assertEquals("https://upsc.gov.in/syllabus", entity.syllabusUrl)

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
        assertEquals(model.totalVacancies, mappedBack.totalVacancies)
        assertEquals(model.vacancyBreakdownJson, mappedBack.vacancyBreakdownJson)
        assertEquals(model.payScaleSummary, mappedBack.payScaleSummary)
        assertEquals(model.applicationFeeGeneral, mappedBack.applicationFeeGeneral)
        assertEquals(model.applicationFeeReserved, mappedBack.applicationFeeReserved)
        assertEquals(model.feeExemptionDetails, mappedBack.feeExemptionDetails)
        assertEquals(model.ageMin, mappedBack.ageMin)
        assertEquals(model.ageMax, mappedBack.ageMax)
        assertEquals(model.ageCutoffDate, mappedBack.ageCutoffDate)
        assertEquals(model.ageRelaxationSummary, mappedBack.ageRelaxationSummary)
        assertEquals(model.educationalQualification, mappedBack.educationalQualification)
        assertEquals(model.hasCorrigendum, mappedBack.hasCorrigendum)
        assertEquals(model.corrigendumSummary, mappedBack.corrigendumSummary)
        assertEquals(model.photoSpecifications, mappedBack.photoSpecifications)
        assertEquals(model.signatureSpecifications, mappedBack.signatureSpecifications)
        assertEquals(model.feePaymentDeadline, mappedBack.feePaymentDeadline)
        assertEquals(model.titleHi, mappedBack.titleHi)
        assertEquals(model.summaryHi, mappedBack.summaryHi)
        assertEquals(model.examStages, mappedBack.examStages)
        assertEquals(model.negativeMarking, mappedBack.negativeMarking)
        assertEquals(model.examMode, mappedBack.examMode)
        assertEquals(model.syllabusUrl, mappedBack.syllabusUrl)
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
