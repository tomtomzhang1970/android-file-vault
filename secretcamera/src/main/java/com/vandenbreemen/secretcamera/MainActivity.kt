package com.vandenbreemen.secretcamera

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.vandenbreemen.mobilesecurestorage.android.api.FileWorkflow
import com.vandenbreemen.mobilesecurestorage.android.fragment.SFSNavFragment
import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials
import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import com.vandenbreemen.secretcamera.mvp.SFSMenuContract
import com.vandenbreemen.secretcamera.mvp.impl.SFSMainMenuModel
import com.vandenbreemen.secretcamera.mvp.impl.SFSMainMenuPresenterImpl
import dagger.android.AndroidInjection

class MainActivity : AppCompatActivity(), SFSMenuContract.SFSMainMenuView {


    override fun onReadyToUse() {

    }

    override fun showError(error: ApplicationError) {
        Toast.makeText(this, error.localizedMessage, LENGTH_SHORT)
    }


    /**
     * File access workflow (containing the file we're going to be working with)
     */
    var fsWorkflow: FileWorkflow? = null

    var mainMenuPresenter: SFSMenuContract.SFSMainMenuPresenter? = null

    override fun onPause() {
        super.onPause()
        mainMenuPresenter?.let { it.close() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        //  Get file workflow
        fsWorkflow = intent.getParcelableExtra(FileWorkflow.PARM_WORKFLOW_NAME)

        if (fsWorkflow == null) {
            fsWorkflow = FileWorkflow()
            fsWorkflow!!.activityToStartAfterTargetActivityFinished = javaClass
        }

        intent.getParcelableExtra<SFSCredentials>(SFSCredentials.PARM_CREDENTIALS)?.let { credentials ->

            findViewById<ViewGroup>(R.id.mainSection).addView(
                    layoutInflater.inflate(R.layout.main_screen_selections, findViewById(R.id.mainSection), false))

            mainMenuPresenter = SFSMainMenuPresenterImpl(SFSMainMenuModel(credentials), this)
        } ?: run {
            val frag = SFSNavFragment()
            frag.workflow = fsWorkflow!!
            frag.setCancelAction(this@MainActivity.javaClass)
            fragmentManager.beginTransaction().add(R.id.upperSection, frag).commit()
        }
    }

    override fun onResume() {
        super.onResume()
        mainMenuPresenter?.let { it.start() }
    }

    fun onTakeNote(view:View){
        mainMenuPresenter!!.takeNote()
    }

    override fun gotoNotesList(credentials:SFSCredentials, strings:ArrayList<String>) {

        val workflow = StringSelectorWorkflow(NoteDetailsActivity::class.java, strings, credentials)
        workflow.setOnCancelActivity(MainActivity::class.java)
        val intent = Intent(this, StringSelectorActivity::class.java)
        intent.putExtra(StringSelectorActivity.WORKFLOW, workflow)
        startActivity(intent)
        finish()

    }

    fun onViewPictures(view: View) {
        mainMenuPresenter!!.openGallery()
    }

    fun onViewNotes(view: View){
        mainMenuPresenter!!.viewNotes()
    }

    fun onTakePicture(view: View) {
        mainMenuPresenter!!.takePicture()
    }

    override fun gotoTakeNote(credentials: SFSCredentials) {
        val takeNote = Intent(this, TakeNoteActivity::class.java)
        takeNote.putExtra(SFSCredentials.PARM_CREDENTIALS, credentials)
        startActivity(takeNote)
        finish()
    }

    override fun gotoTakePicture(credentials: SFSCredentials) {
        val takePicture = Intent(this, TakePictureActivity::class.java)
        takePicture.putExtra(SFSCredentials.PARM_CREDENTIALS, credentials)
        startActivity(takePicture)
        finish()
    }

    override fun gotoGallery(credentials: SFSCredentials) {
        val gallery = Intent(this, Gallery::class.java)
        gallery.putExtra(SFSCredentials.PARM_CREDENTIALS, credentials)
        startActivity(gallery)
        finish()
    }
}
