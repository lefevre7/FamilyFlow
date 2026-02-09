package com.debanshu.xcalendar.domain.usecase.project

import com.debanshu.xcalendar.domain.model.Project
import com.debanshu.xcalendar.domain.repository.IProjectRepository
import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Factory

@Factory
class GetProjectsUseCase(
    private val projectRepository: IProjectRepository,
) {
    operator fun invoke(): Flow<List<Project>> = projectRepository.getProjects()
}
