package com.example.studypilot.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Database(
    entities = [
        UserPreferences::class,
        StudySession::class

    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class StudyPilotDatabase : RoomDatabase() {

    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun studySessionDao(): StudySessionDao


    companion object {
        @Volatile
        private var INSTANCE: StudyPilotDatabase? = null

        fun getDatabase(context: Context): StudyPilotDatabase {
            // Return existing instance or create new one
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPilotDatabase::class.java,
                    "study_pilot_database"
                )
                    // Enable WAL mode for better concurrent access
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    // Allow queries on main thread only for very simple/fast queries
                    // Most operations should still be on background threads
                    .allowMainThreadQueries()  // Use sparingly!
                    // Fallback strategy for migrations
                    .fallbackToDestructiveMigration()
                    // Add callback for initialization
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Perform any initialization here on background thread
                // Example: pre-populate database
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Optimize database on open
                db.execSQL("PRAGMA cache_size = 10000")
                db.execSQL("PRAGMA temp_store = MEMORY")
            }
        }
    }
}