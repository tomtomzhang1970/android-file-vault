package com.vandenbreemen.mobilesecurestorage.android

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vandenbreemen.mobilesecurestorage.R
import com.vandenbreemen.mobilesecurestorage.android.api.FileWorkflow
import com.vandenbreemen.mobilesecurestorage.android.mvp.fileselect.FileSelectController
import com.vandenbreemen.mobilesecurestorage.android.mvp.fileselect.FileSelectModel
import com.vandenbreemen.mobilesecurestorage.android.mvp.fileselect.FileSelectView
import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import java.io.File

class FileSelectActivity : Activity(), FileSelectView, ActivityCompat.OnRequestPermissionsResultCallback {
    override fun display(error: ApplicationError) {
        Toast.makeText(this, error.localizedMessage, Toast.LENGTH_SHORT)
    }

    /**
     * Callback - usually for testing only
     */
    interface FileSelectListener {
        fun onFileSelect(file: File)
    }

    companion object {
        /**
         * Permissions for file IO
         */
        const val PERM_REQUEST_ID = 2501

        /**
         * Indicates that we're selecting a directory only
         */
        const val PARM_DIR_ONLY = "DIR_ONLY"

        /**
         * For selecting files confirmation is not required.  File selection will be done on first
         * file clicked
         */
        const val PARM_NO_CONFIRM_NEEDED = "__NO_CONF_RQD"

        const val PARM_TITLE = "__TITLE"
    }

    /**
     * App controller
     */
    private lateinit var controller: FileSelectController

    private var listener: FileSelectListener = object : FileSelectListener {
        override fun onFileSelect(file: File) {
            onSelectedFile(file)
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_file_select)

        val model = FileSelectModel(this)
        model.setSelectDirectories(intent.getBooleanExtra(PARM_DIR_ONLY, false))
        model.isAutoSelect = intent.getBooleanExtra(PARM_NO_CONFIRM_NEEDED, false)

        val title = intent.getStringExtra(PARM_TITLE)
                ?: resources.getText(R.string.select_file_folder)
        findViewById<TextView>(R.id.title).setText(title)

        controller = FileSelectController(model, this)
    }

    /**
     * Set listener for testing
     */
    protected fun setListener(listener: FileSelectListener) {
        this.listener = listener;
    }

    override fun onResume() {

        super.onResume()

        //  Check for file IO permissions
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    PERM_REQUEST_ID)
        } else {
            controller.start()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray){
        when(requestCode){
            PERM_REQUEST_ID->{
                if((grantResults!!.isNotEmpty() && grantResults!![0] == PackageManager.PERMISSION_GRANTED)){
                    controller.start()
                }
            }
        }
    }

    override fun select(selected: File) {
        listener.onFileSelect(selected)
    }

    private fun onSelectedFile(selected: File) {
        val tmpWorkflow = FileWorkflow()
        tmpWorkflow.fileOrDirectory = selected

        val result = Intent()
        result.putExtra(FileWorkflow.PARM_WORKFLOW_NAME, tmpWorkflow)
        setResult(RESULT_OK, result)
        finish()
    }

    override fun listFiles(files: MutableList<File>?) {

        files?.let { files ->
            val listView = findViewById<ListView>(R.id.fileList);

            val adapter = object : ArrayAdapter<File>(this, android.R.layout.simple_list_item_1) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

                    val group = layoutInflater.inflate(R.layout.layout_file_item, parent, false)
                    group.findViewById<TextView>(R.id.itemLabel).text = (files!![position].name)

                    return group
                }
            }

            listView.setOnItemClickListener { parent, view, position, id ->
                val selected = adapter.getItem(position)
                controller.select(selected)
            }

            adapter.addAll(files)

            listView.adapter = adapter
        }

    }

    fun onOkay(view: View?) {
        controller.confirm()
    }

    fun onCancel(view: View?) {
        setResult(RESULT_CANCELED)
        finish()
    }
}
