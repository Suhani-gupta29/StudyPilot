package com.example.studypilot.data

import androidx.room.TypeConverter
import com.example.studypilot.ui.casual.CasualTask
import com.example.studypilot.ui.focus.BreakType

import com.example.studypilot.ui.mode.FocusNotificationType
import com.example.studypilot.ui.focus.PlanType
import com.example.studypilot.ui.home.FocusSession

import com.example.studypilot.ui.shared.FocusSubject
import com.example.studypilot.ui.shared.FocusTask
import com.example.studypilot.ui.shared.StudySession
import com.example.studypilot.ui.shared.Subject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.studypilot.ui.focus.FocusSubject as FocusFocusSubject
import com.example.studypilot.ui.focus.FocusTask as FocusFocusTask


class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromSubjectList(subjects: List<Subject>?): String? {
        return gson.toJson(subjects)
    }

    @TypeConverter
    fun toSubjectList(subjectsString: String?): List<Subject>? {
        if (subjectsString == null) return null
        val listType = object : TypeToken<List<Subject>>() {}.type
        return gson.fromJson(subjectsString, listType)
    }

    @TypeConverter
    fun fromStudySessionList(sessions: List<StudySession>?): String? {
        return gson.toJson(sessions)
    }

    @TypeConverter
    fun toStudySessionList(sessionsString: String?): List<StudySession>? {
        if (sessionsString == null) return null
        val listType = object : TypeToken<List<StudySession>>() {}.type
        return gson.fromJson(sessionsString, listType)
    }

    @TypeConverter
    fun fromFocusTaskList(tasks: List<FocusTask>?): String? {
        return gson.toJson(tasks)
    }

    @TypeConverter
    fun toFocusTaskList(tasksString: String?): List<FocusTask>? {
        if (tasksString == null) return null
        val listType = object : TypeToken<List<FocusTask>>() {}.type
        return gson.fromJson(tasksString, listType)
    }

    @TypeConverter
    fun fromCasualTaskList(tasks: List<CasualTask>?): String? {
        return gson.toJson(tasks)
    }

    @TypeConverter
    fun toCasualTaskList(tasksString: String?): List<CasualTask>? {
        if (tasksString == null) return null
        val listType = object : TypeToken<List<CasualTask>>() {}.type
        return gson.fromJson(tasksString, listType)
    }

    @TypeConverter
    fun fromFocusSubjectList(subjects: List<FocusSubject>?): String? {
        return gson.toJson(subjects)
    }

    @TypeConverter
    fun toFocusSubjectList(subjectsString: String?): List<FocusSubject>? {
        if (subjectsString == null) return null
        val listType = object : TypeToken<List<FocusSubject>>() {}.type
        return gson.fromJson(subjectsString, listType)
    }

    @TypeConverter
    fun fromFocusNotificationTypeSet(notificationTypes: Set<FocusNotificationType>?): String? {
        return gson.toJson(notificationTypes)
    }

    @TypeConverter
    fun toFocusNotificationTypeSet(notificationTypesString: String?): Set<FocusNotificationType>? {
        if (notificationTypesString == null) return null
        val setType = object : TypeToken<Set<FocusNotificationType>>() {}.type
        return gson.fromJson(notificationTypesString, setType)
    }

    @TypeConverter
    fun fromFocusFocusSubjectList(subjects: List<FocusFocusSubject>?): String? {
        return gson.toJson(subjects)
    }

    @TypeConverter
    fun toFocusFocusSubjectList(subjectsString: String?): List<FocusFocusSubject>? {
        if (subjectsString == null) return null
        val listType = object : TypeToken<List<FocusFocusSubject>>() {}.type
        return gson.fromJson(subjectsString, listType)
    }

    @TypeConverter
    fun fromFocusFocusTaskList(tasks: List<FocusFocusTask>?): String? {
        return gson.toJson(tasks)
    }

    @TypeConverter
    fun toFocusFocusTaskList(tasksString: String?): List<FocusFocusTask>? {
        if (tasksString == null) return null
        val listType = object : TypeToken<List<FocusFocusTask>>() {}.type
        return gson.fromJson(tasksString, listType)
    }

    @TypeConverter
    fun fromPlanType(planType: PlanType?): String? {
        return planType?.name
    }

    @TypeConverter
    fun toPlanType(planTypeString: String?): PlanType? {
        return planTypeString?.let { PlanType.valueOf(it) }
    }

    @TypeConverter
    fun fromBreakType(breakType: BreakType?): String? {
        return breakType?.name
    }

    @TypeConverter
    fun toBreakType(breakTypeString: String?): BreakType? {
        return breakTypeString?.let { BreakType.valueOf(it) }
    }

    @TypeConverter
    fun fromFocusSessionList(value: List<FocusSession>?): String {
        return gson.toJson(value ?: emptyList<FocusSession>())
    }

    @TypeConverter
    fun toFocusSessionList(value: String): List<FocusSession> {
        val listType = object : TypeToken<List<FocusSession>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromExemptedSessions(map: Map<String, List<Int>>?): String? {
        return gson.toJson(map)
    }

    @TypeConverter
    fun toExemptedSessions(json: String?): Map<String, List<Int>>? {
        if (json == null) return null
        val type = object : TypeToken<Map<String, List<Int>>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromStringListMap(value: Map<String, List<String>>?): String {
        return gson.toJson(value ?: emptyMap<String, List<String>>())
    }

    @TypeConverter
    fun toStringListMap(value: String?): Map<String, List<String>> {
        if (value.isNullOrBlank()) return emptyMap()
        val type = object : TypeToken<Map<String, List<String>>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return gson.toJson(value ?: emptyList<String>())
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
