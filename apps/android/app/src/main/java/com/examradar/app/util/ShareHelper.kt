package com.examradar.app.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import com.examradar.app.data.model.CalendarEventModel
import com.examradar.app.data.model.DateType
import com.examradar.app.data.model.NoticeModel
import java.text.NumberFormat
import java.util.Locale

/**
 * Utility helper for formatted, high-conversion WhatsApp and Telegram study circle broadcasts.
 * Formats official notices, urgent deadlines, and exam countdowns with deep links for zero-cost organic growth.
 */
object ShareHelper {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    private const val TELEGRAM_PACKAGE = "org.telegram.messenger"

    const val APP_BASE_URL = "https://app.examradar.in"

    /**
     * Formats an official recruitment/exam notification into a rich, structured WhatsApp/Telegram card.
     */
    fun formatNoticeShareCard(notice: NoticeModel): String {
        val deadline = notice.importantDates.find { it.dateType == DateType.APPLICATION_DEADLINE }?.eventDate
        val deadlineStr = if (deadline != null) DateFormatter.format(deadline) else "Check Official Notification"

        return buildString {
            append("📢 *OFFICIAL NOTICE: ${notice.title.trim()}*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🏛️ *Authority:* ${notice.organization}\n")

            if (notice.totalVacancies != null && notice.totalVacancies > 0) {
                val formattedVacancies = NumberFormat.getIntegerInstance(Locale("en", "IN")).format(notice.totalVacancies)
                append("👥 *Total Vacancies:* $formattedVacancies Posts\n")
            }

            if (!notice.payScaleSummary.isNullOrBlank()) {
                append("💰 *Pay Scale:* ${notice.payScaleSummary.trim()}\n")
            }

            append("⏳ *Last Date to Apply:* $deadlineStr\n")

            if (notice.examDate != null) {
                append("📅 *Examination Date:* ${DateFormatter.format(notice.examDate)}\n")
            }

            if (!notice.educationalQualification.isNullOrBlank()) {
                append("🎓 *Qualification:* ${notice.educationalQualification.trim()}\n")
            }

            if (notice.ageMin != null && notice.ageMax != null) {
                append("🎂 *Age Criteria:* ${notice.ageMin} – ${notice.ageMax} Years\n")
            }

            if (notice.applicationFeeGeneral != null) {
                val feeGeneral = if (notice.applicationFeeGeneral == 0) "Free" else "₹${notice.applicationFeeGeneral}"
                val feeReserved = if (notice.applicationFeeReserved != null) {
                    if (notice.applicationFeeReserved == 0) "Free (Exempt)" else "₹${notice.applicationFeeReserved}"
                } else "Exempt for reserved categories"
                append("💳 *Application Fee:* $feeGeneral (Reserved: $feeReserved)\n")
            }

            if (!notice.officialApplyUrl.isNullOrBlank()) {
                append("\n🔗 *Apply Online Portal:*\n${notice.officialApplyUrl.trim()}\n")
            }

            if (!notice.officialNoticeUrl.isNullOrBlank()) {
                append("📄 *Official Gazette PDF:*\n${notice.officialNoticeUrl.trim()}\n")
            }

            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("⚡ *100% Official & Verified Source*\n")
            append("📲 *Read Official PDF & Track Deadlines on ExamRadar:*\n")
            append("$APP_BASE_URL/notices/${notice.id}")
        }
    }

    /**
     * Formats an urgent application deadline notice card (for T-48h and T-24h warnings).
     */
    fun formatUrgentDeadlineShareCard(notice: NoticeModel, daysLeft: Long): String {
        val daysText = if (daysLeft <= 1) "FINAL 24 HOURS" else "$daysLeft DAYS LEFT"
        val deadlineDateStr = notice.applicationDeadline?.let { DateFormatter.format(it) } ?: "Ending Soon"

        return buildString {
            append("🚨 *URGENT APPLICATION DEADLINE: $daysText!* 🚨\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🎯 *Target Exam:* ${notice.examName}\n")
            append("🏛️ *Authority:* ${notice.organization}\n")
            append("⏰ *Window Closes:* $deadlineDateStr (5:00 PM)\n")

            if (notice.totalVacancies != null && notice.totalVacancies > 0) {
                val formattedVacancies = NumberFormat.getIntegerInstance(Locale("en", "IN")).format(notice.totalVacancies)
                append("👥 *Vacancies at Stake:* $formattedVacancies Posts\n")
            }

            append("\n⚠️ *Reminder:* Government application servers crash heavily in the last 24 hours. Submit application fee and download confirmation slip immediately!\n")

            if (!notice.officialApplyUrl.isNullOrBlank()) {
                append("\n🔗 *Direct Apply Link:*\n${notice.officialApplyUrl.trim()}\n")
            }

            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📲 *Stay Ahead with Live Exam Radar:*\n")
            append("$APP_BASE_URL/notices/${notice.id}")
        }
    }

    /**
     * Formats an exam countdown card for sharing daily discipline in study circles.
     */
    fun formatExamCountdownShareCard(examName: String, daysLeft: Long, examDateStr: String?): String {
        return buildString {
            append("⏳ *EXAM RADAR COUNTDOWN: ${examName.trim()}*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            if (!examDateStr.isNullOrBlank()) {
                append("📅 *Scheduled Date:* $examDateStr\n")
            }
            append("🚨 *Days Remaining:* $daysLeft Days Left\n")
            append("🎯 *Daily Reminder:* Focus on revision, previous year questions (PYQs), and mock tests.\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("⚡ Free Official Exam Alerts & Countdown Widget:\n")
            append(APP_BASE_URL)
        }
    }

    /**
     * Formats a calendar event card for sharing exam dates, admit card drops, or deadlines.
     */
    fun formatCalendarEventShareCard(event: CalendarEventModel): String {
        val typeLabel = when (event.dateType) {
            DateType.APPLICATION_START -> "Registration Opens"
            DateType.APPLICATION_DEADLINE -> "Application Deadline"
            DateType.CORRECTION_WINDOW -> "Correction Window"
            DateType.EXAM_DATE -> "Examination Date"
            DateType.ADMIT_CARD_RELEASE -> "Admit Card Release"
            DateType.RESULT_DATE -> "Result Declaration"
        }
        val statusTag = when {
            event.isRevised -> " [REVISED DATE]"
            event.isTentative -> " [TENTATIVE]"
            else -> ""
        }
        return buildString {
            append("📅 *EXAM TIMELINE UPDATE: ${event.examName.trim()}*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("🏛️ *Authority:* ${event.organization}\n")
            append("🔔 *Event:* $typeLabel$statusTag\n")
            append("📆 *Date:* ${DateFormatter.format(event.eventDate)}\n")
            if (!event.officialNoticeUrl.isNullOrBlank()) {
                append("📄 *Official Circular:* ${event.officialNoticeUrl.trim()}\n")
            }
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("⚡ *Track Live Exam Calendar on ExamRadar App:*\n")
            append("$APP_BASE_URL/notices/${event.noticeId}")
        }
    }

    /**
     * Formats a study checklist milestone card.
     */
    fun formatChecklistProgressShareCard(examName: String, completedCount: Int, totalCount: Int): String {
        val percentage = if (totalCount > 0) (completedCount * 100) / totalCount else 0
        return buildString {
            append("📚 *Preparation Milestone for ${examName.trim()}*\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("✅ *Completed:* $completedCount / $totalCount Official Topics ($percentage% Done)\n")
            append("🔥 Consistent daily progress leads to selection.\n")
            append("━━━━━━━━━━━━━━━━━━━━\n")
            append("📲 Track official syllabus checklist offline on ExamRadar:\n")
            append(APP_BASE_URL)
        }
    }

    /**
     * Check if a specific package is installed on device.
     */
    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 1-Tap direct share to WhatsApp or WhatsApp Business.
     * Falls back to generic share if neither is installed.
     */
    fun shareToWhatsApp(context: Context, text: String): Boolean {
        val targetPackage = when {
            isPackageInstalled(context, WHATSAPP_PACKAGE) -> WHATSAPP_PACKAGE
            isPackageInstalled(context, WHATSAPP_BUSINESS_PACKAGE) -> WHATSAPP_BUSINESS_PACKAGE
            else -> null
        }

        if (targetPackage != null) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(targetPackage)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Fall back to generic chooser if direct intent fails
            }
        }

        shareGeneric(context, text, "Share with Study Group via...")
        return false
    }

    /**
     * 1-Tap direct share to Telegram.
     * Falls back to generic share if not installed.
     */
    fun shareToTelegram(context: Context, text: String): Boolean {
        if (isPackageInstalled(context, TELEGRAM_PACKAGE)) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    setPackage(TELEGRAM_PACKAGE)
                    putExtra(Intent.EXTRA_TEXT, text)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return true
            } catch (e: Exception) {
                // Fall back to generic chooser if direct intent fails
            }
        }

        shareGeneric(context, text, "Share with Study Group via...")
        return false
    }

    /**
     * Standard Android share sheet chooser for any app.
     */
    fun shareGeneric(context: Context, text: String, chooserTitle: String = "Share with Study Group") {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooserIntent = Intent.createChooser(sendIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }

    /**
     * Copy formatted card text to device clipboard with instant candidate toast feedback.
     */
    fun copyToClipboard(
        context: Context,
        text: String,
        toastMessage: String = "Card copied! Ready to paste into your WhatsApp/Telegram group."
    ) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("ExamRadar Study Card", text)
        clipboard?.setPrimaryClip(clip)
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }
}
