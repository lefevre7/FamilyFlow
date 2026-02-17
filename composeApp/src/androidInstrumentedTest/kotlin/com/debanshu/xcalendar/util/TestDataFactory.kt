package com.debanshu.xcalendar.util

import com.debanshu.xcalendar.domain.model.Person
import com.debanshu.xcalendar.domain.model.PersonRole
import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.model.TaskPriority
import com.debanshu.xcalendar.domain.model.TaskStatus
import com.debanshu.xcalendar.domain.model.TaskEnergy

/**
 * Test data factory for instrumented tests.
 * Extends commonTest TestDataFactory with additional builders for Person, Task, etc.
 */
object TestDataFactory {
    
    fun createPerson(
        id: String = "test-person-${System.currentTimeMillis()}",
        name: String = "Test Person",
        role: PersonRole = PersonRole.CHILD,
        color: Int = 0xFF2196F3.toInt()
    ) = Person(
        id = id,
        name = name,
        role = role,
        color = color
    )
    
    fun createTask(
        id: String = "test-task-${System.currentTimeMillis()}",
        title: String = "Test Task",
        notes: String? = null,
        status: TaskStatus = TaskStatus.OPEN,
        priority: TaskPriority = TaskPriority.SHOULD,
        affectedPersonIds: List<String> = emptyList()
    ) = Task(
        id = id,
        title = title,
        notes = notes,
        status = status,
        priority = priority,
        affectedPersonIds = affectedPersonIds,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
