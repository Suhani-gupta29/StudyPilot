package com.example.studypilot

import android.app.Application
import androidx.room.Room
import com.example.studypilot.data.StudyPilotDatabase
import com.example.studypilot.data.UserPreferencesRepository
import com.example.studypilot.data.SessionRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class StudyPilotApplication : Application() {

    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Lazy initialization of database - only created when first accessed
    val database: StudyPilotDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            StudyPilotDatabase::class.java,
            "study_pilot_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    // Lazy initialization of repositories
    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(database.userPreferencesDao())
    }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(database.studySessionDao())
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase on background thread
        applicationScope.launch(Dispatchers.IO) {
            try {
                FirebaseApp.initializeApp(this@StudyPilotApplication)
            } catch (e: Exception) {
                // Firebase already initialized or initialization failed
                e.printStackTrace()
            }
        }

        // Pre-warm database connection on background thread (optional but recommended)
        applicationScope.launch(Dispatchers.IO) {
            try {
                database.openHelper.writableDatabase
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onTerminate() {
        super.onTerminate()
        // Close database when app terminates
        if (database.isOpen) {
            database.close()
        }
    }
}