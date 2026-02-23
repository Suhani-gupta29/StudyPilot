package com.example.studypilot.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.studypilot.ui.focus.FocusDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFocusDetails(focusDetails: FocusDetails)

    @Query("SELECT * FROM focus_details ORDER BY id DESC LIMIT 1")
    fun getFocusDetails(): Flow<FocusDetails?>
}
