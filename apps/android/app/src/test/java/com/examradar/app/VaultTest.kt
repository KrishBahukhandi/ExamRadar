package com.examradar.app

import com.examradar.app.data.local.ApplicationVaultEntity
import com.examradar.app.util.VaultStorageHelper
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class VaultTest {

    @Test
    fun testVaultEntityCreationAndDefaults() {
        val entry = ApplicationVaultEntity(
            id = "test-uuid-1",
            examId = "exam-upsc-cse",
            examName = "UPSC Civil Services Examination 2026",
            applicationNumber = "UPSC2026-981245",
            rollNumber = "0823419",
            examCenterName = "Govt Model Sr Sec School, Sector 19",
            examCenterCity = "Chandigarh",
            examDate = LocalDate.of(2026, 5, 24),
            reportingTime = "08:30 AM",
            shiftDetails = "Shift 1 (GS-I: 09:30 - 11:30 AM)",
            admitCardFileName = "UPSC_CSE_AdmitCard_0823419.pdf",
            admitCardFilePath = "/data/user/0/com.examradar.app/files/vault_documents/admit_card_test.pdf",
            notes = "Carry 2 black ballpoint pens and original Aadhaar card"
        )

        assertEquals("test-uuid-1", entry.id)
        assertEquals("UPSC Civil Services Examination 2026", entry.examName)
        assertEquals("UPSC2026-981245", entry.applicationNumber)
        assertEquals("0823419", entry.rollNumber)
        assertEquals("Govt Model Sr Sec School, Sector 19", entry.examCenterName)
        assertEquals("Chandigarh", entry.examCenterCity)
        assertEquals(LocalDate.of(2026, 5, 24), entry.examDate)
        assertEquals("08:30 AM", entry.reportingTime)
        assertEquals("Shift 1 (GS-I: 09:30 - 11:30 AM)", entry.shiftDetails)
        assertEquals("UPSC_CSE_AdmitCard_0823419.pdf", entry.admitCardFileName)
        assertEquals("Carry 2 black ballpoint pens and original Aadhaar card", entry.notes)
        assertTrue(entry.createdAt > 0)
        assertTrue(entry.updatedAt > 0)
    }

    @Test
    fun testWhatsAppShareCardFormatting() {
        val entry = ApplicationVaultEntity(
            id = "test-uuid-2",
            examName = "SSC CGL 2026 Tier-1",
            applicationNumber = "SSC2026881920",
            rollNumber = "2201098432",
            examCenterName = "iON Digital Zone iDZ 1, Noida Sector 62",
            examCenterCity = "Noida",
            examDate = LocalDate.of(2026, 9, 15),
            reportingTime = "07:45 AM",
            shiftDetails = "Shift 1 (09:00 - 10:00 AM)"
        )

        val shareCard = VaultStorageHelper.formatShareText(entry)

        assertTrue(shareCard.contains("ExamRadar Application Details"))
        assertTrue(shareCard.contains("Exam:* SSC CGL 2026 Tier-1"))
        assertTrue(shareCard.contains("Application No:* `SSC2026881920`"))
        assertTrue(shareCard.contains("Roll No:* `2201098432`"))
        assertTrue(shareCard.contains("Center:* iON Digital Zone iDZ 1, Noida Sector 62, Noida"))
        assertTrue(shareCard.contains("Exam Date:* 15 September 2026"))
        assertTrue(shareCard.contains("Reporting Time:* 07:45 AM"))
        assertTrue(shareCard.contains("Shift/Session:* Shift 1 (09:00 - 10:00 AM)"))
        assertTrue(shareCard.contains("ExamRadar App"))
    }

    @Test
    fun testVaultSearchFiltering() {
        val list = listOf(
            ApplicationVaultEntity(
                id = "1",
                examName = "UPSC CSE 2026",
                applicationNumber = "APP-111",
                rollNumber = "ROLL-999",
                examCenterName = "Delhi Center"
            ),
            ApplicationVaultEntity(
                id = "2",
                examName = "SSC CGL 2026",
                applicationNumber = "APP-222",
                rollNumber = "ROLL-888",
                examCenterName = "Patna Center"
            ),
            ApplicationVaultEntity(
                id = "3",
                examName = "IBPS PO 2026",
                applicationNumber = "APP-333",
                rollNumber = "ROLL-777",
                examCenterName = "Jaipur Center"
            )
        )

        fun filterEntries(query: String): List<ApplicationVaultEntity> {
            if (query.isBlank()) return list
            val q = query.lowercase().trim()
            return list.filter {
                it.examName.lowercase().contains(q) ||
                it.applicationNumber.lowercase().contains(q) ||
                (it.rollNumber?.lowercase()?.contains(q) == true) ||
                (it.examCenterName?.lowercase()?.contains(q) == true)
            }
        }

        // Test filter by roll number
        assertEquals(1, filterEntries("999").size)
        assertEquals("1", filterEntries("999")[0].id)

        // Test filter by city/center
        assertEquals(1, filterEntries("patna").size)
        assertEquals("2", filterEntries("patna")[0].id)

        // Test filter by exam name
        assertEquals(1, filterEntries("ibps").size)
        assertEquals("3", filterEntries("ibps")[0].id)

        // Test blank query
        assertEquals(3, filterEntries("").size)

        // Test nonexistent query
        assertEquals(0, filterEntries("rrb alp").size)
    }
}
