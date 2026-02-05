package com.example.studypilot.data

import com.example.studypilot.ui.mode.ModeSelectionState
import com.example.studypilot.ui.mode.StudyMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class UserPreferencesRepository(private val userPreferencesDao: UserPreferencesDao) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val userFlows = ConcurrentHashMap<String, MutableStateFlow<UserPreferences?>>()
    private val protectedUntil = ConcurrentHashMap<String, Long>()

    fun getUserPreferences(userId: String): Flow<UserPreferences?> {
        userFlows[userId]?.let { return it.asStateFlow() }

        val state = MutableStateFlow<UserPreferences?>(null)
        userFlows[userId] = state

        scope.launch {
            userPreferencesDao.getUserPreferences(userId).collect { daoPrefs ->
                val current = state.value

                if (daoPrefs == null) return@collect

                if (current == null) {
                    if (hasMeaningfulContent(daoPrefs)) state.value = daoPrefs
                    return@collect
                }

                val now = System.currentTimeMillis()
                if (now < (protectedUntil[current.userId] ?: 0L)) {
                    if (!hasMeaningfulContent(daoPrefs) && hasMeaningfulContent(current)) {
                        return@collect
                    }
                }

                if (daoPrefs.lastAccessed >= current.lastAccessed) {
                    state.value = daoPrefs
                }
            }
        }

        return state.asStateFlow()
    }

    private fun hasMeaningfulContent(prefs: UserPreferences): Boolean {
        return when (prefs.selectedMode) {
            StudyMode.EXAM -> prefs.examSubjects.isNotEmpty() && !isPlaceholderSubjects(prefs.examSubjects)
            StudyMode.FOCUS -> prefs.focusSubjects.isNotEmpty()
            StudyMode.CASUAL -> prefs.casualSubjects.isNotEmpty()
            else -> false
        }
    }


    suspend fun saveUserPreferences(userPreferences: UserPreferences) {
        val now = System.currentTimeMillis()
        val incoming = userPreferences.copy(lastAccessed = now)

        val dbCurrent = userPreferencesDao.getUserPreferences(incoming.userId).firstOrNull()

        val merged = if (dbCurrent == null) {
            incoming
        } else {
            val examSubjects = if (incoming.examSubjects.isNotEmpty()) incoming.examSubjects else dbCurrent.examSubjects
            val casualSubjects = if (incoming.casualSubjects.isNotEmpty()) incoming.casualSubjects else dbCurrent.casualSubjects
            val dailyPlan = if (incoming.dailyPlan.isNotEmpty()) incoming.dailyPlan else dbCurrent.dailyPlan

            val examName = incoming.examName ?: dbCurrent.examName
            val examDate = incoming.examDate ?: dbCurrent.examDate
            val planStartDate = incoming.planStartDate ?: dbCurrent.planStartDate

            dbCurrent.copy(
                selectedMode = incoming.selectedMode,
                examPreferences = incoming.examPreferences,
                focusPreferences = incoming.focusPreferences,
                casualPreferences = incoming.casualPreferences,
                lastAccessed = now,
                lastRoute = incoming.lastRoute ?: dbCurrent.lastRoute,
                examName = examName,
                examDate = examDate,
                planStartDate = planStartDate,
                examSubjects = examSubjects,
                casualSubjects = casualSubjects,
                dailyPlan = dailyPlan,
                dailyPlanDate = incoming.dailyPlanDate ?: dbCurrent.dailyPlanDate,
                tasks = if (incoming.tasks.isNotEmpty()) incoming.tasks else dbCurrent.tasks,
                casualTasks = if (incoming.casualTasks.isNotEmpty()) incoming.casualTasks else dbCurrent.casualTasks,
                planType = incoming.planType ?: dbCurrent.planType,
                excludeSunday = incoming.excludeSunday ?: dbCurrent.excludeSunday,
                focusSubjects = if (incoming.focusSubjects.isNotEmpty()) incoming.focusSubjects else dbCurrent.focusSubjects
            )
        }

        userPreferencesDao.saveUserPreferences(merged)

        userFlows.compute(merged.userId) { _, existing ->
            if (existing == null) MutableStateFlow(merged)
            else {
                existing.value = merged
                existing
            }
        }
        protectedUntil[merged.userId] = System.currentTimeMillis() + 2000L
    }

    fun getStudyModePreferences(userId: String): Flow<ModeSelectionState?> {
        return getUserPreferences(userId).map { userPrefs ->
            userPrefs?.let {
                ModeSelectionState(
                    selectedMode = it.selectedMode,
                    examPreferences = it.examPreferences,
                    focusPreferences = it.focusPreferences,
                    casualPreferences = it.casualPreferences
                )
            }
        }
    }

    suspend fun saveStudyModePreferences(userId: String, modeSelectionState: ModeSelectionState) {
        val currentPrefs = getUserPreferences(userId).firstOrNull()
        android.util.Log.d("UserPrefsRepo", "saveStudyModePreferences incoming for user=$userId: $modeSelectionState; currentPrefs=$currentPrefs")
        val newUserPrefs = (currentPrefs ?: UserPreferences(userId = userId, selectedMode = modeSelectionState.selectedMode, examPreferences = modeSelectionState.examPreferences, focusPreferences = modeSelectionState.focusPreferences, casualPreferences = modeSelectionState.casualPreferences, lastAccessed = 0))
            .copy(
                selectedMode = modeSelectionState.selectedMode,
                examPreferences = modeSelectionState.examPreferences,
                focusPreferences = modeSelectionState.focusPreferences,
                casualPreferences = modeSelectionState.casualPreferences,
                lastAccessed = System.currentTimeMillis()
            )
        saveUserPreferences(newUserPrefs)
        userFlows[userId]?.let {
            android.util.Log.d("UserPrefsRepo", "saveStudyModePreferences persisted for user=$userId: ${it.value}")
        }
    }

    suspend fun cleanupOrphanedProfiles() {
        val thirtyDaysAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L
        userPreferencesDao.deleteOrphanedProfiles(thirtyDaysAgo)
    }

    private fun isPlaceholderSubjects(subjects: List<com.example.studypilot.ui.shared.Subject>): Boolean {
        if (subjects.size != 1) return false
        val name = subjects[0].name.trim().lowercase()
        return name == "general study" || name == "free study"
    }
}
