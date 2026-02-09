package com.debanshu.xcalendar.domain.usecase.task

import com.debanshu.xcalendar.domain.model.Task
import com.debanshu.xcalendar.domain.repository.ITaskRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetTasksUseCase(
    private val taskRepository: ITaskRepository,
) {
    operator fun invoke(): Flow<List<Task>> = taskRepository.getTasks()
}
