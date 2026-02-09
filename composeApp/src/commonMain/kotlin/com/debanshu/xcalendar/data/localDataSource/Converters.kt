package com.debanshu.xcalendar.data.localDataSource

import androidx.room.TypeConverter
import com.debanshu.xcalendar.domain.model.InboxSource
import com.debanshu.xcalendar.domain.model.InboxStatus
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.ProjectStatus
import com.debanshu.xcalendar.domain.model.RoutineTimeOfDay
import com.debanshu.xcalendar.domain.model.TaskEnergy
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.TaskType

class Converters {
    @TypeConverter
    fun fromPersonRole(role: PersonRole): String = role.name

    @TypeConverter
    fun toPersonRole(value: String): PersonRole = PersonRole.valueOf(value)

    @TypeConverter
    fun fromTaskStatus(status: TaskStatus): String = status.name

    @TypeConverter
    fun toTaskStatus(value: String): TaskStatus = TaskStatus.valueOf(value)

    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(value: String): TaskPriority = TaskPriority.valueOf(value)

    @TypeConverter
    fun fromTaskEnergy(energy: TaskEnergy): String = energy.name

    @TypeConverter
    fun toTaskEnergy(value: String): TaskEnergy = TaskEnergy.valueOf(value)

    @TypeConverter
    fun fromTaskType(type: TaskType): String = type.name

    @TypeConverter
    fun toTaskType(value: String): TaskType = TaskType.valueOf(value)

    @TypeConverter
    fun fromRoutineTimeOfDay(value: RoutineTimeOfDay): String = value.name

    @TypeConverter
    fun toRoutineTimeOfDay(value: String): RoutineTimeOfDay = RoutineTimeOfDay.valueOf(value)

    @TypeConverter
    fun fromProjectStatus(value: ProjectStatus): String = value.name

    @TypeConverter
    fun toProjectStatus(value: String): ProjectStatus = ProjectStatus.valueOf(value)

    @TypeConverter
    fun fromInboxStatus(value: InboxStatus): String = value.name

    @TypeConverter
    fun toInboxStatus(value: String): InboxStatus = InboxStatus.valueOf(value)

    @TypeConverter
    fun fromInboxSource(value: InboxSource): String = value.name

    @TypeConverter
    fun toInboxSource(value: String): InboxSource = InboxSource.valueOf(value)

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("|")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}
