package com.vandenbreemen.secretcamera.mvp.impl.projects

import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.Presenter
import com.vandenbreemen.secretcamera.api.Task
import com.vandenbreemen.secretcamera.mvp.projects.ProjectDetailsPresenter
import com.vandenbreemen.secretcamera.mvp.projects.ProjectDetailsRouter
import com.vandenbreemen.secretcamera.mvp.projects.ProjectDetailsView

class ProjectDetailsPresenterImpl(val projectDetailsModel: ProjectDetailsModel, val projectDetailsView: ProjectDetailsView, val projectDetailsRouter: ProjectDetailsRouter) :
        Presenter<ProjectDetailsModel, ProjectDetailsView>(projectDetailsModel, projectDetailsView),
        ProjectDetailsPresenter {


    override fun setupView() {
        projectDetailsView.showDescription(projectDetailsModel.getDescription())
        projectDetailsView.showName(projectDetailsModel.getProjectTitle())
        projectDetailsView.displayTasks(projectDetailsModel.getTasks())
    }

    override fun returnToMain() {
        projectDetailsRouter.returnToMain(projectDetailsModel.copyCredentials())
    }

    override fun selectAddTask() {
        projectDetailsRouter.showTaskDetails(null)
    }

    override fun submitUpdateTaskDetails(existingTask: Task, updateTaskData: Task) {
        try {
            addForDisposal(
                    projectDetailsModel.submitUpdateTaskDetails(existingTask, updateTaskData).subscribe({ tasks ->
                        projectDetailsView.displayTasks(tasks)
                    }, { error ->
                        if (error is ApplicationError) {
                            projectDetailsView.showError(error)
                        } else {
                            projectDetailsView.showError(ApplicationError("Unknown error occurred"))
                        }
                    })
            )
        } catch (err: ApplicationError) {
            projectDetailsView.showError(err)
        }
    }

    override fun setCompleted(task: Task, completed: Boolean) {
        addForDisposal(
                projectDetailsModel.markTaskCompleted(task, completed).subscribe { tasks ->
                    projectDetailsView.displayTasks(tasks)
                }
        )
    }

    override fun viewTask(task: Task) {
        projectDetailsRouter.showTaskDetails(task)
    }

    override fun submitTaskDetails(task: Task) {
        try {
            addForDisposal(
                    projectDetailsModel.addTask(task).subscribe({ taskList ->
                        projectDetailsView.displayTasks(taskList)
                    }, { error ->
                        if (error is ApplicationError) {
                            projectDetailsView.showError(error)
                        } else {
                            error.printStackTrace()
                            projectDetailsView.showError(ApplicationError("Unknown error occurred"))
                        }
                    })
            )
        } catch (err: ApplicationError) {
            projectDetailsView.showError(err)
        }
    }

    override fun selectProjectDetails() {
        projectDetailsRouter.displayProjectDetails(projectDetailsModel.project)
    }

    override fun submitUpdatedProjectDetails(name: String, projectDescription: String) {
        try {
            addForDisposal(
                    projectDetailsModel.submitUpdatedProjectDetails(name, projectDescription).subscribe({ project ->
                        projectDetailsView.showDescription(project.details)
                        projectDetailsView.showName(project.title)
                    }, { error ->
                        if (error is ApplicationError) {
                            projectDetailsView.showError(error)
                        } else {
                            error.printStackTrace()
                            projectDetailsView.showError(ApplicationError("Unknown error occurred"))
                        }
                    })
            )
        } catch (err: ApplicationError) {
            projectDetailsView.showError(err)
        }
    }

    override fun notifyItemMoved(oldPosition: Int, newPosition: Int) {
        addForDisposal(
                projectDetailsModel.updateItemPosition(oldPosition, newPosition).subscribe({}, { error ->
                    if (error is ApplicationError) {
                        projectDetailsView.showError(error)
                    }
                })
        )
    }
}
