package com.vandenbreemen.secretcamera

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.*
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.vandenbreemen.mobilesecurestorage.android.sfs.SFSCredentials
import com.vandenbreemen.mobilesecurestorage.android.view.EnterPasswordView
import com.vandenbreemen.mobilesecurestorage.file.api.FileInfo
import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.Pausable
import com.vandenbreemen.secretcamera.di.injectPictureViewer
import com.vandenbreemen.secretcamera.fragment.ConfirmDeleteDialogFragment
import com.vandenbreemen.secretcamera.fragment.ThumbnailsFragment
import com.vandenbreemen.secretcamera.mvp.gallery.*
import kotlinx.android.synthetic.main.activity_picture_viewer.*
import kotlinx.android.synthetic.main.file_info_dialog.view.*
import kotlinx.android.synthetic.main.file_info_dialog.view.fileName
import kotlinx.android.synthetic.main.image_select_item.view.*
import kotlinx.coroutines.*
import java.io.File
import javax.inject.Inject

class ThumbnailViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view) {

    /**
     * Current running coroutine for fetching the thumbnail
     */
    var thumbnailJob: Job? = null

}

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

    override fun onViewRecycled(holder: ThumbnailViewHolder) {
        super.onViewRecycled(holder)

        holder.view.preview.setImageBitmap(null)
        holder.view.preview.visibility = GONE

        holder.thumbnailJob?.cancel()
        holder.thumbnailJob = null

    }

    override fun onBindViewHolder(holder: ThumbnailViewHolder, position: Int) {
        val loadingSpinner = holder.view.findViewById<ProgressBar>(R.id.loading)
        loadingSpinner.visibility = VISIBLE

        holder.view.fileName.text = fileNames[position]

        holder.view.tag = fileNames[position]

        holder.thumbnailJob = CoroutineScope(Dispatchers.Default).launch {
            val bitmap = presenter.fetchThumbnail(fileNames[position]) ?: return@launch

            withContext(Dispatchers.Main) {

                val imageView = holder.view.findViewById<ImageView>(R.id.preview)
                imageView.setOnClickListener { presenter.selectImageToDisplay(fileNames[position]) }
                imageView.setImageBitmap(bitmap)
                imageView.visibility = VISIBLE

                //  Image select checkbox
                holder.view.findViewById<CheckBox>(R.id.checkBox).visibility = if (selectEnabled) VISIBLE else GONE
                if (selectEnabled) {
                    val checkbox = holder.view.findViewById<CheckBox>(R.id.checkBox)
                    checkbox.isChecked = presenter.selected(fileNames[position])
                    checkbox.setOnClickListener { _ -> presenter.selectImage(fileNames[position]) }
                } else {   //  Allow turning on multiselect
                    imageView.setOnLongClickListener {
                        presenter.toggleSelectImages()
                        true
                    }
                }

                loadingSpinner.visibility = GONE
            }

        }
    }

}

class PictureViewerActivity : AppCompatActivity(), PictureViewerView, PictureViewRouter, Pausable, ThumbnailsFragment.ThumbnailScreenListener {

    companion object {
        /**
         * Delete confirmation dialog tag
         */
        const val TAG_DELETE_DIALOG = "delete_confirmation"
    }

    @Inject
    lateinit var presenter: PictureViewerPresenter

    private var pictureViewRouterDelegate: PictureViewRouter? = null

    private val dialogs = mutableListOf<Dialog>()

    override fun onCreate(savedInstanceState: Bundle?) {
        injectPictureViewer(this)
        super.onCreate(savedInstanceState)

        //  See also https://developer.android.com/training/system-ui/status.html
        val decorView = window.decorView
        val uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
        decorView.systemUiVisibility = uiOptions

        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        setContentView(R.layout.activity_picture_viewer)


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

        //  Set up long-press
        findViewById<SubsamplingScaleImageView>(R.id.currentImage).setOnLongClickListener { view ->
            presenter.showCurrentFileInfo()
            true
        }

    }

    override fun onBackPressed() {
        presenter.returnToMain()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            presenter.returnToMain()
        }

        return true
    }

    override fun onResume() {
        super.onResume()
        presenter.start()
    }

    override fun onCancel() {
        dismissAllDialogs()
    }

    private fun dismissAllDialogs() {

        dialog_container.removeAllViews()
        pictureViewRouterDelegate = null
        dialog_container.visibility = GONE
        dialog_container.y = 10000f

        dialogs.forEach { dialog ->
            dialog.dismiss()
        }

        dialogs.clear()

        (supportFragmentManager.findFragmentByTag(TAG_DELETE_DIALOG) as? DialogFragment)?.apply {
            dismiss()
        }
    }


    override fun onPause() {
        super.onPause()

        //  Force current image to be dropped
        findViewById<SubsamplingScaleImageView>(R.id.currentImage).recycle()
        dismissAllDialogs()

        //  Proceed with standard cleanup etc.
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
        }, {
            finish()
            startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        })

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

    override fun displayFileInfo(fileInfo: FileInfo) {

        val builder = AlertDialog.Builder(this)
        val detailView = layoutInflater.inflate(R.layout.file_info_dialog, null)
        detailView.fileName.text = fileInfo.fileName
        detailView.fileSize.text = fileInfo.size.toString()

        builder.setView(detailView)

        val view: Dialog = builder.create()
        view.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialogs.add(view)
        view.show()


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
            val frag = ThumbnailsFragment(files, currentImageFilename, presenter)

            dialog_container.visibility = VISIBLE
            dialog_container.animate().translationY(0f).setDuration(500).start()
            supportFragmentManager.beginTransaction().add(R.id.dialog_container, frag).commit()

        }


    }

    override fun hideImageSelector() {
        dismissAllDialogs()
    }

    override fun navigateBack(sfsCredentials: SFSCredentials) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
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

    override fun showPictureViewerActions() {
        findViewById<ViewGroup>(R.id.actionsWindow).visibility = VISIBLE
    }

    override fun hidePictureViewerActions() {
        findViewById<ViewGroup>(R.id.actionsWindow).visibility = GONE
    }

    override fun showActions() {
        pictureViewRouterDelegate?.let { it.showActions() }
    }

    override fun hideActions() {
        pictureViewRouterDelegate?.let { it.hideActions() }
    }

    override fun enableSelectMultiple() {
        pictureViewRouterDelegate?.let { it.enableSelectMultiple() }
    }

    override fun disableSelectMultiple() {
        pictureViewRouterDelegate?.let { it.disableSelectMultiple() }
    }

    override fun providePictureViewRouterDelegate(router: PictureViewRouter) {
        this.pictureViewRouterDelegate = router
    }

    override fun confirmDeleteFiles(filesToDelete: List<String>) {

        val dialog = ConfirmDeleteDialogFragment(fileNames = filesToDelete) {
            presenter.confirmDeleteSelected()
        }
        dialog.show(supportFragmentManager, TAG_DELETE_DIALOG)

    }
}
