package com.example.studypilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.studypilot.ui.focus.FocusDetails

@Database(
    entities = [UserPreferences::class, FocusDetails::class, StudySession::class],
    version = 22,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPilotDatabase : RoomDatabase() {

    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun focusDetailsDao(): FocusDetailsDao
    abstract fun studySessionDao(): StudySessionDao

    companion object {
        @Volatile
        private var INSTANCE: StudyPilotDatabase? = null

        fun getDatabase(context: Context): StudyPilotDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPilotDatabase::class.java,
                    "studypilot_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}