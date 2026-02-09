package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.model.asTask
import com.debanshu.xcalendar.common.model.asTaskEntity
import com.debanshu.xcalendar.data.localDataSource.TaskDao
import com.debanshu.xcalendar.domain.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [ITaskRepository::class])
class TaskRepository(
    private val taskDao: TaskDao,
) : BaseRepository(), ITaskRepository {
    override fun getTasks(): Flow<List<Task>> =
        safeFlow(
            flowName = "getTasks",
            defaultValue = emptyList(),
            flow = taskDao.getAllTasks().map { entities -> entities.map { it.asTask() } },
        )

    override suspend fun getTaskById(taskId: String): Task? =
        safeCallOrThrow("getTaskById($taskId)") {
            taskDao.getTaskById(taskId)?.asTask()
        }

    override suspend fun upsertTask(task: Task) =
        safeCallOrThrow("upsertTask(${task.id})") {
            taskDao.upsertTask(task.asTaskEntity())
        }

    override suspend fun upsertTasks(tasks: List<Task>) =
        safeCallOrThrow("upsertTasks(${tasks.size})") {
            taskDao.upsertTasks(tasks.map { it.asTaskEntity() })
        }

    override suspend fun deleteTask(task: Task) =
        safeCallOrThrow("deleteTask(${task.id})") {
            taskDao.deleteTask(task.asTaskEntity())
        }
}
