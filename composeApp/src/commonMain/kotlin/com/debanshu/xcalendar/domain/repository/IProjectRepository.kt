package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.domain.model.Project
import kotlinx.coroutines.flow.Flow

interface IProjectRepository {
    fun getProjects(): Flow<List<Project>>
    suspend fun getProjectById(projectId: String): Project?
    suspend fun upsertProject(project: Project)
    suspend fun upsertProjects(projects: List<Project>)
    suspend fun deleteProject(project: Project)
}
