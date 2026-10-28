package com.examradar.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * Primary Room SQLite database for ExamRadar Android.
 * Provides offline-first persistence for official government exam notices,
 * exam catalogs, and recruitment categories.
 */
@Database(
    entities = [
        CategoryEntity::class,
        ExamEntity::class,
        NoticeEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ExamRadarDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun examDao(): ExamDao
    abstract fun noticeDao(): NoticeDao

    companion object {
        @Volatile
        private var INSTANCE: ExamRadarDatabase? = null

        fun getInstance(context: Context): ExamRadarDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ExamRadarDatabase::class.java,
                    "examradar.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
