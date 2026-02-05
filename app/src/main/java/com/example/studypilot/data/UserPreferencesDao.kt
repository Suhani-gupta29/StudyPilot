package com.example.studypilot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserPreferences(userPreferences: UserPreferences)

    @Query("SELECT * FROM user_preferences WHERE userId = :userId")
    fun getUserPreferences(userId: String): Flow<UserPreferences?>

    @Query("DELETE FROM user_preferences WHERE lastAccessed < :threshold")
    suspend fun deleteOrphanedProfiles(threshold: Long)
}
