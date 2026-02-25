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
    version = 6,
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
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StudyPilotDatabase::class.java,
                    "study_pilot_database"
                )
                    .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                    .allowMainThreadQueries()
                    .addMigrations(
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6
                    )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN focusWeeklySubjectPriorities TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN casualWeeklySubjectPriorities TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN examOverDialogShownForExamDate TEXT"
                )
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN examPostSubjects TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN displayName TEXT"
                )
            }
        }

        val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Stores "Do It Today" moved sessions per date so the button stays
                // hidden even after logout or app restart.
                // Encoded as a JSON map (Map<String, List<String>>) via Converters.
                database.execSQL(
                    "ALTER TABLE user_preferences ADD COLUMN focusDoItTodaySessions TEXT NOT NULL DEFAULT '{}'"
                )
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA cache_size = 10000")
                db.execSQL("PRAGMA temp_store = MEMORY")
            }
        }
    }
}