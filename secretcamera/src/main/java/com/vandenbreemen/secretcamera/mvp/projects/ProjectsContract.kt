package com.vandenbreemen.secretcamera.mvp.projects

import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.PresenterContract
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.View
import com.vandenbreemen.secretcamera.api.Project
import com.vandenbreemen.secretcamera.api.Task

/*

VIPER-based pattern for managing projects

 */

interface ProjectListPresenter : PresenterContract {
    fun addProject(project: Project)
    fun viewProjectDetails(project: Project)
}

interface ProjectListView : View {
    fun showProjects(projects: List<Project>)
}

interface ProjectListRouter {
    fun gotoProjectDetails(projectName: String, credentials: SFSCredentials)

}

interface ProjectDetailsPresenter : PresenterContract {
    fun selectAddTask()
    fun submitTaskDetails(task: Task)
    fun viewTask(task: Task)
    fun submitUpdateTaskDetails(existingTask: Task, updateTaskData: Task)
    fun setCompleted(task: Task, completed: Boolean)

}

interface ProjectDetailsView : View {
    fun showDescription(description: String)
    fun showName(title: String)
    fun displayTasks(tasks: List<Task>)
}

interface ProjectDetailsRouter {
    fun showTaskDetails(task: Task?)

}
