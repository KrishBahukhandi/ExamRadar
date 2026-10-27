package com.examradar.app.data.local

import androidx.room.TypeConverter
import com.examradar.app.data.model.DateType
import com.examradar.app.data.model.NoticeType
import com.examradar.app.data.model.VerificationStatus
import java.time.LocalDate

/**
 * Type converters for Room database persistence.
 */
class Converters {

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    }

    @TypeConverter
    fun fromNoticeType(type: NoticeType?): String? = type?.name

    @TypeConverter
    fun toNoticeType(value: String?): NoticeType? = value?.let {
        runCatching { NoticeType.valueOf(it) }.getOrDefault(NoticeType.RECRUITMENT)
    }

    @TypeConverter
    fun fromVerificationStatus(status: VerificationStatus?): String? = status?.name

    @TypeConverter
    fun toVerificationStatus(value: String?): VerificationStatus? = value?.let {
        runCatching { VerificationStatus.valueOf(it) }.getOrDefault(VerificationStatus.PENDING)
    }

    @TypeConverter
    fun fromDateType(type: DateType?): String? = type?.name

    @TypeConverter
    fun toDateType(value: String?): DateType? = value?.let {
        runCatching { DateType.valueOf(it) }.getOrDefault(DateType.APPLICATION_DEADLINE)
    }
}
