package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface ITaskRepository {
    fun getTasks(): Flow<List<Task>>
    suspend fun getTaskById(taskId: String): Task?
    suspend fun upsertTask(task: Task)
    suspend fun upsertTasks(tasks: List<Task>)
    suspend fun deleteTask(task: Task)
}
