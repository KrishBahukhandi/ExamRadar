package com.examradar.app

import com.examradar.app.data.cache.OfflineDiskCache
import com.examradar.app.data.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.time.LocalDate

class OfflineDiskCacheTest {

    private lateinit var tempDir: File
    private lateinit var diskCache: OfflineDiskCache

    @Before
    fun setUp() {
        tempDir = Files.createTempDirectory("examradar_test_cache").toFile()
        diskCache = OfflineDiskCache(tempDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun saveAndLoadNotices_persistsCorrectly() {
        val notice = NoticeModel(
            id = "test-notice-1",
            examId = "exam-upsc-cse",
            examName = "Civil Services Exam 2026",
            organization = "UPSC",
            noticeType = NoticeType.RECRUITMENT,
            title = "Official Notification Release",
            summary = "Notification has been released for CSE 2026.",
            officialNoticeUrl = "https://upsc.gov.in/notice1",
            status = "PUBLISHED",
            verificationStatus = VerificationStatus.VERIFIED,
            publishedDate = LocalDate.of(2026, 2, 14),
            applicationDeadline = LocalDate.of(2026, 3, 5),
            importantDates = listOf(
                ImportantDateModel(
                    dateType = DateType.EXAM_DATE,
                    eventDate = LocalDate.of(2026, 5, 24)
                )
            ),
            isDemonstrationFixture = false
        )

        diskCache.saveNotices(listOf(notice))

        val loaded = diskCache.loadNotices()
        assertNotNull(loaded)
        assertEquals(1, loaded!!.size)
        val loadedNotice = loaded[0]
        assertEquals("test-notice-1", loadedNotice.id)
        assertEquals("UPSC", loadedNotice.organization)
        assertEquals(NoticeType.RECRUITMENT, loadedNotice.noticeType)
        assertEquals(VerificationStatus.VERIFIED, loadedNotice.verificationStatus)
        assertEquals(LocalDate.of(2026, 2, 14), loadedNotice.publishedDate)
        assertEquals(LocalDate.of(2026, 3, 5), loadedNotice.applicationDeadline)
        assertEquals(1, loadedNotice.importantDates.size)
        assertEquals(DateType.EXAM_DATE, loadedNotice.importantDates[0].dateType)
        assertEquals(LocalDate.of(2026, 5, 24), loadedNotice.importantDates[0].eventDate)

        val meta = diskCache.getMeta()
        assertEquals(1, meta.noticesCount)
        assertTrue(meta.lastSyncTimestamp > 0)
        assertFalse(diskCache.isStale())
    }

    @Test
    fun saveAndLoadExamsAndCategories_persistsCorrectly() {
        val category = CategoryModel(
            id = "cat-upsc",
            name = "Civil Services",
            slug = "civil-services",
            description = "Union Public Service Commission exams",
            examCount = 1
        )
        val exam = ExamModel(
            id = "exam-upsc-cse",
            categoryId = "cat-upsc",
            organization = "UPSC",
            examName = "Civil Services Examination",
            examCode = "CSE",
            officialPortalUrl = "https://upsc.gov.in",
            description = "Premier recruitment for IAS, IPS, and IFS.",
            activeNoticeCount = 1
        )

        diskCache.saveCategories(listOf(category))
        diskCache.saveExams(listOf(exam))

        val loadedCats = diskCache.loadCategories()
        val loadedExams = diskCache.loadExams()

        assertNotNull(loadedCats)
        assertEquals(1, loadedCats!!.size)
        assertEquals("cat-upsc", loadedCats[0].id)
        assertEquals("Civil Services", loadedCats[0].name)

        assertNotNull(loadedExams)
        assertEquals(1, loadedExams!!.size)
        assertEquals("exam-upsc-cse", loadedExams[0].id)
        assertEquals("CSE", loadedExams[0].examCode)
    }

    @Test
    fun clear_removesAllCachedFiles() {
        diskCache.saveNotices(listOf(
            NoticeModel(
                id = "n1",
                examId = "e1",
                examName = "Exam",
                organization = "Org",
                noticeType = NoticeType.RECRUITMENT,
                title = "Title",
                summary = "Summary",
                officialNoticeUrl = "https://example.com",
                status = "PUBLISHED",
                verificationStatus = VerificationStatus.VERIFIED,
                publishedDate = LocalDate.now(),
                applicationDeadline = null
            )
        ))

        assertNotNull(diskCache.loadNotices())
        diskCache.clear()

        assertNull(diskCache.loadNotices())
        assertNull(diskCache.loadExams())
        assertNull(diskCache.loadCategories())
    }
}
