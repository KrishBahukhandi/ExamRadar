package com.examradar.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM categories")
    suspend fun clearCategories()
}

@Dao
interface ExamDao {

    @Query("SELECT * FROM exams ORDER BY examName ASC")
    fun getAllExams(): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams ORDER BY examName ASC")
    suspend fun getAllExamsList(): List<ExamEntity>

    @Query("SELECT * FROM exams WHERE categoryId = :categoryId ORDER BY examName ASC")
    fun getExamsByCategory(categoryId: String): Flow<List<ExamEntity>>

    @Query("SELECT * FROM exams WHERE id = :examId")
    fun getExamById(examId: String): Flow<ExamEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExams(exams: List<ExamEntity>)

    @Query("DELETE FROM exams")
    suspend fun clearExams()
}

@Dao
interface NoticeDao {

    @Query("SELECT * FROM notices ORDER BY publishedDate DESC, lastCheckedTimestamp DESC")
    fun getAllNotices(): Flow<List<NoticeEntity>>

    @Query("SELECT * FROM notices ORDER BY publishedDate DESC, lastCheckedTimestamp DESC")
    suspend fun getAllNoticesList(): List<NoticeEntity>

    @Query("SELECT * FROM notices WHERE id = :noticeId")
    fun getNoticeById(noticeId: String): Flow<NoticeEntity?>

    @Query("SELECT * FROM notices WHERE examId = :examId ORDER BY publishedDate DESC")
    fun getNoticesByExamId(examId: String): Flow<List<NoticeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotices(notices: List<NoticeEntity>)

    @Query("DELETE FROM notices")
    suspend fun clearNotices()
}
