package com.example.studypilot.data

import androidx.room.TypeConverter
import com.example.studypilot.ui.mode.FocusNotificationType

class TypeConverters {
    @TypeConverter
    fun fromFocusNotificationTypeSet(notificationTypes: Set<FocusNotificationType>): String {
        return notificationTypes.joinToString(",") { it.name }
    }

    @TypeConverter
    fun toFocusNotificationTypeSet(data: String): Set<FocusNotificationType> {
        if (data.isEmpty()) return emptySet()
        return data.split(",").map { FocusNotificationType.valueOf(it) }.toSet()
    }
}
