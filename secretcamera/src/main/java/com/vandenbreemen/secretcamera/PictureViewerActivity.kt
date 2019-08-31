package com.vandenbreemen.secretcamera

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials
import com.vandenbreemen.mobilesecurestorage.android.view.EnterPasswordView
import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.Pausable
import com.vandenbreemen.secretcamera.di.injectPictureViewer
import com.vandenbreemen.secretcamera.mvp.gallery.*
import java.io.File
import javax.inject.Inject

class ThumbnailViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view)

class ThumbnailAdapter(private val fileNames: List<String>,
                       private val presenter: PictureViewerPresenter
) : RecyclerView.Adapter<ThumbnailViewHolder>() {

    var selectEnabled: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThumbnailViewHolder {
        return ThumbnailViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.image_select_item, parent, false) as ViewGroup)
    }

    override fun getItemCount(): Int {
        return fileNames.size
    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val loadingSpinner = holder.view.findViewById<ProgressBar>(R.id.loading)

        presenter.thumbnail(fileNames[position]).subscribe ({ bitmap ->
            val imageView = holder.view.findViewById<ImageView>(R.id.preview)
            imageView.setOnClickListener { presenter.selectImageToDisplay(fileNames[position]) }
            imageView.visibility = VISIBLE
            imageView.setImageBitmap(bitmap)

            //  Image select checkbox
            holder.view.findViewById<CheckBox>(R.id.checkBox).visibility = if (selectEnabled) VISIBLE else GONE
            if (selectEnabled) {
                val checkbox = holder.view.findViewById<CheckBox>(R.id.checkBox)
                checkbox.isChecked = presenter.selected(fileNames[position])
                checkbox.setOnClickListener { v -> presenter.selectImage(fileNames[position]) }
            } else {   //  Allow turning on multiselect
                imageView.setOnLongClickListener {
                    presenter.toggleSelectImages()
                    true
                }
            }


            loadingSpinner.visibility = GONE
        }, {error ->

            loadingSpinner.visibility = GONE
        })
    }

}

class PictureViewerActivity : Activity(), PictureViewerView, PictureViewRouter, Pausable {


    @Inject
    lateinit var presenter: PictureViewerPresenter

    private var adapter: ThumbnailAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        injectPictureViewer(this)
        super.onCreate(savedInstanceState)

        //  See also https://developer.android.com/training/system-ui/status.html
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_picture_viewer)

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = RecyclerView.HORIZONTAL
        val recyclerView = findViewById<RecyclerView>(R.id.pictureSelector)
        recyclerView.layoutManager = layoutManager
        recyclerView.visibility = GONE

        //  Set up the actions
        findViewById<ViewGroup>(R.id.pictureViewerActions).findViewById<Button>(R.id.cancel).setOnClickListener {
            presenter.toggleSelectImages()
        }
        findViewById<ViewGroup>(R.id.pictureViewerActions).findViewById<Button>(R.id.delete).setOnClickListener {
            presenter.deleteSelected()
        }

        //  Actions button was here.  Need to re-create it sometime

        findViewById<Button>(R.id.deleteAllImages).setOnClickListener {
            presenter.deleteAllImages()
        }

    }

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun onPause() {
        super.onPause()
        findViewById<ViewGroup>(R.id.overlay).visibility = VISIBLE
        presenter.pause()
    }

    override fun pauseWithFileOpen(fileLocation: File) {

        findViewById<SubsamplingScaleImageView>(R.id.currentImage).visibility = GONE
        hideImageSelector()
        val overlay = findViewById<ViewGroup>(R.id.overlay)
        val enterPasswordView = overlay.findViewById<EnterPasswordView>(R.id.enter_password_view)
        enterPasswordView.promptForPasswordOnResume(fileLocation, { sfsCredentials ->
            presenter = PictureViewerPresenterImpl(PictureViewerModel(sfsCredentials), this, this)
            overlay.visibility = GONE
            findViewById<SubsamplingScaleImageView>(R.id.currentImage).visibility = VISIBLE
            presenter.start()
        }, { finish() })

        findViewById<SubsamplingScaleImageView>(R.id.currentImage).recycle()
    }

    override fun onReadyToUse() {
        findViewById<ViewGroup>(R.id.overlay).visibility = GONE
        presenter.displayCurrentImage()
    }

    override fun showError(error: ApplicationError) {
        runOnUiThread {
            Toast.makeText(this, error.localizedMessage, LENGTH_SHORT).show()
        }
    }

    override fun displayImage(imageToDisplay: Bitmap) {
        findViewById<SubsamplingScaleImageView>(R.id.currentImage).maxScale = 15F
        findViewById<SubsamplingScaleImageView>(R.id.currentImage).setImage(ImageSource.bitmap(imageToDisplay))
    }

    fun onForward(view: View) {
        presenter.nextImage()
    }

    fun onBack(view: View) {
        presenter.previousImage()
    }

    fun onShowSelector(view: View) {
        presenter.showSelector()
    }

    override fun showImageSelector(files: List<String>) {
        presenter.currentImageFileName().subscribe { currentImageFilename ->
            val recyclerView = findViewById<RecyclerView>(R.id.pictureSelector)
            val adapter = ThumbnailAdapter(files, presenter)
            this.adapter = adapter

            recyclerView.adapter = adapter
            recyclerView.layoutManager?.scrollToPosition(files.indexOf(currentImageFilename))
            (recyclerView.adapter as ThumbnailAdapter).notifyDataSetChanged()
            recyclerView.visibility = VISIBLE
        }


    }

    override fun hideImageSelector() {
        val recyclerView = findViewById<RecyclerView>(R.id.pictureSelector)
        recyclerView.removeAllViews()
        recyclerView.visibility = GONE
        adapter = null
    }

    override fun navigateBack(sfsCredentials: SFSCredentials) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(SFSCredentials.PARM_CREDENTIALS, sfsCredentials)
        startActivity(intent)
    }

    override fun end() {
        findViewById<ViewGroup>(R.id.overlay).visibility = VISIBLE
    }

    override fun showLoadingSpinner() {
        findViewById<View>(R.id.imageDisplayProgress).visibility = VISIBLE
    }

    override fun hideLoadingSpinner() {
        findViewById<View>(R.id.imageDisplayProgress).visibility = GONE
    }

    override fun showActions() {
        findViewById<ViewGroup>(R.id.pictureViewerActions).visibility = VISIBLE
    }

    override fun hideActions() {
        findViewById<ViewGroup>(R.id.pictureViewerActions).visibility = GONE
    }

    override fun showPictureViewerActions() {
        findViewById<ViewGroup>(R.id.actionsWindow).visibility = VISIBLE
    }

    override fun hidePictureViewerActions() {
        findViewById<ViewGroup>(R.id.actionsWindow).visibility = GONE
    }

    override fun enableSelectMultiple() {
        adapter!!.selectEnabled = true
        adapter!!.notifyDataSetChanged()
    }

    override fun disableSelectMultiple() {
        adapter!!.selectEnabled = false
        adapter!!.notifyDataSetChanged()
    }
}
