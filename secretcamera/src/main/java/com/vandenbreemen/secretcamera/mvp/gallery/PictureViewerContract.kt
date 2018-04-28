package com.vandenbreemen.secretcamera.mvp.gallery

import android.graphics.Bitmap
import android.util.Log
import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.Presenter
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.PresenterContract
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.View
import com.vandenbreemen.mobilesecurestorage.security.crypto.listFiles
import com.vandenbreemen.mobilesecurestorage.security.crypto.persistence.SecureFileSystem
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import io.reactivex.schedulers.Schedulers.computation


class ImageFilesInteractor(private val sfs: SecureFileSystem) {
    fun listImageFiles(): List<String> {
        return sfs.listFiles(PicturesFileTypes.IMPORTED_IMAGE).sorted()
    }

    fun loadImageBytes(fileName: String): ByteArray {
        try {
            return sfs.loadBytesFromFile(fileName)
        } catch (exception: Exception) {
            Log.e(ImageFilesInteractor::class.java.simpleName, "Failed to load image bytes", exception)
            throw ApplicationError("Error loading $fileName")
        }
    }

}

interface PictureViewerView : View {
    fun displayImage(imageToDisplay: Bitmap)
    fun showImageSelector(files: List<String>)
    fun end()
    fun hideImageSelector()
}

interface PictureViewerPresenter : PresenterContract {
    fun selectImageToDisplay()
    fun nextImage()
    fun previousImage()
    fun showSelector()
    fun thumbnail(fileName: String): Single<Bitmap>
    fun selectImageToDisplay(fileName: String)
    fun currentImageFileName(): Single<String>

}

class PictureViewerPresenterImpl(val model: PictureViewerModel, val view: PictureViewerView) : Presenter<PictureViewerModel, PictureViewerView>(model, view), PictureViewerPresenter {
    override fun selectImageToDisplay(fileName: String) {
        model.loadImage(fileName).subscribe({ image ->
            view.displayImage(image)
            view.hideImageSelector()
        })
    }


    override fun selectImageToDisplay() {

        model.currentFile().flatMap { imageFile ->
            model.loadImage(imageFile)
        }.subscribe({ bitmap -> view.displayImage(bitmap) },
                { error -> view.showError(ApplicationError(error)) }
        )
    }

    override fun nextImage() {
        model.nextFile().flatMap { imageFile ->
            model.loadImage(imageFile)
        }
                .observeOn(mainThread())
                .subscribeOn(computation())
                .subscribe({ bitmap -> view.displayImage(bitmap) },
                { error -> view.showError(ApplicationError(error)) }
        )
    }

    override fun previousImage() {
        model.prevFile().flatMap { imageFile ->
            model.loadImage(imageFile)
        }.subscribe({ bitmap -> view.displayImage(bitmap) },
                { error -> view.showError(ApplicationError(error)) }
        )
    }

    override fun thumbnail(fileName: String): Single<Bitmap> {
        return model.loadImage(fileName).observeOn(mainThread())
                .flatMap { bitmap ->
                    model.getThumbnail(bitmap)
                }.observeOn(mainThread())
                .subscribeOn(computation())
    }

    override fun showSelector() {
        model.listImages().subscribe({ imageFiles -> view.showImageSelector(imageFiles) })
    }

    override fun currentImageFileName(): Single<String> {
        return model.currentFile()
    }

    override fun setupView() {

    }

}