package com.vandenbreemen.secretcamera.mvp.impl.projects

import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials
import com.vandenbreemen.mobilesecurestorage.file.api.SecureFileSystemInteractor
import com.vandenbreemen.mobilesecurestorage.file.api.getSecureFileSystemInteractor
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.Model
import com.vandenbreemen.secretcamera.api.Project
import io.reactivex.Completable
import io.reactivex.Single

class ProjectListModel(credentials: SFSCredentials): Model(credentials) {

    lateinit var sfsInteractor: SecureFileSystemInteractor

    override fun onClose() {

    }

    override fun setup() {
        sfsInteractor = getSecureFileSystemInteractor(sfs)
    }

    fun getProjects(): Single<List<Project>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun addNewProject(project: Project): Completable{
        return Completable.create { subscriber->

            var projectTitle = project.title

            sfsInteractor.save(project, projectTitle, ProjectFileTypes.PROJECT)
        }
    }


}