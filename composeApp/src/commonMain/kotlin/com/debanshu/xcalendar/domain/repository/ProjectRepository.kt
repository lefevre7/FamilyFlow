package com.debanshu.xcalendar.domain.repository

import com.debanshu.xcalendar.common.model.asProject
import com.debanshu.xcalendar.common.model.asProjectEntity
import com.debanshu.xcalendar.data.localDataSource.ProjectDao
import com.debanshu.xcalendar.domain.model.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single

@Single(binds = [IProjectRepository::class])
class ProjectRepository(
    private val projectDao: ProjectDao,
) : BaseRepository(), IProjectRepository {
    override fun getProjects(): Flow<List<Project>> =
        safeFlow(
            flowName = "getProjects",
            defaultValue = emptyList(),
            flow = projectDao.getAllProjects().map { entities -> entities.map { it.asProject() } },
        )

    override suspend fun getProjectById(projectId: String): Project? =
        safeCallOrThrow("getProjectById($projectId)") {
            projectDao.getProjectById(projectId)?.asProject()
        }

    override suspend fun upsertProject(project: Project) =
        safeCallOrThrow("upsertProject(${project.id})") {
            projectDao.upsertProject(project.asProjectEntity())
        }

    override suspend fun upsertProjects(projects: List<Project>) =
        safeCallOrThrow("upsertProjects(${projects.size})") {
            projectDao.upsertProjects(projects.map { it.asProjectEntity() })
        }

    override suspend fun deleteProject(project: Project) =
        safeCallOrThrow("deleteProject(${project.id})") {
            projectDao.deleteProject(project.asProjectEntity())
        }
}
