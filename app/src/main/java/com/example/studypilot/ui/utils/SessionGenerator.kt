package com.example.studypilot.utils

import com.example.studypilot.ui.shared.Difficulty
import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.Priority
import com.example.studypilot.ui.shared.Subject
import kotlin.math.floor
import kotlin.math.max

object SessionGenerator {

    private const val MAX_CONSECUTIVE = 2

    /**
     * Generate exam/focus mode sessions with weight-based distribution
     * Returns list of subject names in the exact order they should appear
     */
    fun generateExamSessionOrder(
        subjects: List<Subject>,
        totalSessions: Int
    ): List<String> {
        android.util.Log.d("SessionGenerator", "generateExamSessionOrder called with ${subjects.size} subjects, totalSessions=$totalSessions")
        android.util.Log.d("SessionGenerator", "Subjects: ${subjects.map { it.name }}")

        if (subjects.isEmpty()) {
            android.util.Log.e("SessionGenerator", "No subjects provided - returning empty list!")
            return emptyList()
        }

        val subjectWeights = subjects.associateWith { subj ->
            val dW = when(subj.difficulty) {
                Difficulty.Hard -> 3
                Difficulty.Medium -> 2
                Difficulty.Easy -> 1
            }
            val pW = when(subj.priority) {
                Priority.High -> 4
                Priority.Medium -> 2
                Priority.Low -> 1
            }
            dW * pW
        }

        android.util.Log.d("SessionGenerator", "Subject weights: $subjectWeights")

        val sessionsPerSubject = mutableMapOf<Subject, Int>()

        if (totalSessions >= subjects.size) {
            subjects.forEach { sessionsPerSubject[it] = 1 }
            var remaining = totalSessions - subjects.size

            val sortedByWeight = subjects.sortedByDescending { subjectWeights[it] ?: 1 }
            var idx = 0
            while (remaining > 0) {
                val subj = sortedByWeight[idx % sortedByWeight.size]
                sessionsPerSubject[subj] = sessionsPerSubject.getOrDefault(subj, 0) + 1
                remaining--
                idx++
            }
        } else {
            val topSubjects = subjects.sortedByDescending { subjectWeights[it] ?: 1 }.take(totalSessions)
            topSubjects.forEach { sessionsPerSubject[it] = 1 }
        }

        android.util.Log.d("SessionGenerator", "Sessions per subject: ${sessionsPerSubject.mapKeys { it.key.name }}")

        val rawSubjects = mutableListOf<Subject>()
        val subjectsSortedForOrder = subjects.sortedByDescending { subjectWeights[it] ?: 1 }
        subjectsSortedForOrder.forEach { subj ->
            val count = sessionsPerSubject.getOrDefault(subj, 0)
            repeat(count) { rawSubjects.add(subj) }
        }

        if (rawSubjects.isEmpty()) {
            rawSubjects.add(subjects.first())
        }

        val limitedSubjects = limitConsecutiveSubjects(rawSubjects)

        val finalSubjects = mutableListOf<Subject>()
        var pointer = 0
        while (finalSubjects.size < totalSessions) {
            finalSubjects.add(limitedSubjects[pointer % limitedSubjects.size])
            pointer++
        }

        val result = finalSubjects.map { it.name }
        android.util.Log.d("SessionGenerator", "Final session order: $result")
        return result
    }

    /**
     * Generate focus mode sessions with weight-based distribution
     */
    fun generateFocusSessionOrder(
        subjects: List<FocusSubject>,
        totalSessions: Int
    ): List<String> {
        if (subjects.isEmpty()) return emptyList()

        val difficultyWeights = mapOf(Difficulty.Hard to 3, Difficulty.Medium to 2, Difficulty.Easy to 1)
        val priorityWeights = mapOf(Priority.High to 3, Priority.Medium to 2, Priority.Low to 1)

        val subjectWeights = subjects.associateWith { subj ->
            (difficultyWeights[subj.difficulty] ?: 1) * (priorityWeights[subj.priority] ?: 1)
        }

        val sessionsPerSubject = mutableMapOf<FocusSubject, Int>()
        subjects.forEach { sessionsPerSubject[it] = 1 }

        var remainingSessions = totalSessions - subjects.size

        if (remainingSessions > 0 && subjectWeights.values.sum() > 0) {
            val sortedByWeight = subjects.sortedByDescending { subjectWeights[it] ?: 1 }
            var idx = 0
            while (remainingSessions > 0) {
                val subject = sortedByWeight[idx % sortedByWeight.size]
                sessionsPerSubject[subject] = sessionsPerSubject.getOrDefault(subject, 0) + 1
                remainingSessions--
                idx++
            }
        }

        val allSessions: MutableList<FocusSubject> = mutableListOf()
        sessionsPerSubject.forEach { (subject, count) -> repeat(count) { allSessions.add(subject) } }

        if (allSessions.isEmpty()) return emptyList()

        allSessions.sortWith(compareByDescending<FocusSubject> { subjectWeights[it] ?: 0 }
            .thenBy { priorityWeights[it.priority] ?: 0 })

        val balancedSubjects = limitConsecutiveSubjects(allSessions.map {
            Subject(it.name, it.priority, it.difficulty)
        })

        val finalSubjects = balancedSubjects.mapNotNull { subj ->
            subjects.find { it.name == subj.name }
        }.toMutableList()

        if (finalSubjects.isEmpty()) return emptyList()

        val result = mutableListOf<FocusSubject>()
        var pointer = 0
        while (result.size < max(1, totalSessions)) {
            result.add(finalSubjects[pointer % finalSubjects.size])
            pointer++
        }

        return result.map { it.name }
    }

    /**
     * Generate casual mode sessions with simple round-robin
     */
    fun generateCasualSessionOrder(
        subjectNames: List<String>,
        totalSessions: Int
    ): List<String> {
        android.util.Log.d("SessionGenerator", "generateCasualSessionOrder called with ${subjectNames.size} subjects, totalSessions=$totalSessions")
        android.util.Log.d("SessionGenerator", "Subject names: $subjectNames")

        if (subjectNames.isEmpty()) {
            android.util.Log.e("SessionGenerator", "No subject names provided - returning empty list!")
            return emptyList()
        }

        val result = List(totalSessions) { i -> subjectNames[i % subjectNames.size] }
        android.util.Log.d("SessionGenerator", "Generated casual order: $result")
        return result
    }

    private fun limitConsecutiveSubjects(subjects: List<Subject>): List<Subject> {
        if (subjects.size <= MAX_CONSECUTIVE) return subjects

        val result = mutableListOf<Subject>()
        var consecutiveCount = 0
        var last: Subject? = null

        for (subject in subjects) {
            if (subject == last) {
                consecutiveCount++
                if (consecutiveCount >= MAX_CONSECUTIVE) {
                    val alternative = subjects.firstOrNull { it != subject }
                    if (alternative != null) {
                        result.add(alternative)
                        last = alternative
                        consecutiveCount = 1
                        continue
                    }
                }
            } else {
                consecutiveCount = 1
            }

            result.add(subject)
            last = subject
        }

        return result
    }

    fun deriveSessions(dailyHours: Float, sessionMinutes: Int): Int {
        val h = if (dailyHours <= 0f) 0.5f else dailyHours
        val m = if (sessionMinutes < 1) 1 else sessionMinutes
        return floor(h.toDouble() * 60.0 / m.toDouble()).toInt().coerceAtLeast(1)
    }
}