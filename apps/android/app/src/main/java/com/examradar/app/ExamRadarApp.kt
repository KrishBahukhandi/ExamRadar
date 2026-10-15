package com.examradar.app

import android.app.Application
import com.examradar.app.data.preferences.UserPreferencesRepository
import com.examradar.app.data.repository.ExamRadarRepository
import com.examradar.app.data.repository.ExamRadarRepositoryImpl

class ExamRadarApp : Application() {

    lateinit var repository: ExamRadarRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val prefsRepository = UserPreferencesRepository(applicationContext)
        repository = ExamRadarRepositoryImpl(prefsRepository)
    }
}
