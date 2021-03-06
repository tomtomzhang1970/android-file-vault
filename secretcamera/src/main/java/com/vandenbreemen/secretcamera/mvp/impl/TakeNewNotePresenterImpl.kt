package com.vandenbreemen.secretcamera.mvp.impl

import android.util.Log
import com.vandenbreemen.mobilesecurestorage.message.ApplicationError
import com.vandenbreemen.mobilesecurestorage.patterns.mvp.Presenter
import com.vandenbreemen.secretcamera.mvp.notes.TakeNewNotePresenter
import com.vandenbreemen.secretcamera.mvp.notes.TakeNewNoteView
import com.vandenbreemen.standardandroidlogging.log.SystemLog
import io.reactivex.Single
import io.reactivex.SingleOnSubscribe
import io.reactivex.schedulers.Schedulers.computation

/**
 * <h2>Intro
 *
 * <h2>Other Details
 * @author kevin
 */
class TakeNewNotePresenterImpl(val view: TakeNewNoteView, val model: TakeNewNoteModel) :
        Presenter<TakeNewNoteModel, TakeNewNoteView>(model, view),
        TakeNewNotePresenter {


    override fun setupView() {

    }

    override fun provideNoteDetails(title: String?, note: String?) {
        try {
            addForDisposal(model.submitNewNote(title ?: "", note ?: "")
                    .subscribe({ _ ->
                        Log.d("TakeNewNotePresenter", "New note taken - ${Thread.currentThread()}")
                        view.onNoteSucceeded("New note created")
                        view.close(model.copyCredentials())
                    },
                            { failure ->
                                Log.e("UnexpectedError", "Error storing new note", failure)
                                view.showError(ApplicationError("Unexpected error"))
                                view.close(model.copyCredentials())
                            }
                    ))
        } catch (error: ApplicationError) {
            view.showError(error)
        }
    }

    override fun saveAndClose(noteTitle: String?, noteContent: String?): Single<Unit> {
        try {
            addForDisposal(return model.submitNewNote(noteTitle ?: "", noteContent
                    ?: "").observeOn(computation()).subscribeOn(computation())
                    .flatMap { unit ->
                        Single.create<Unit>(SingleOnSubscribe {
                            model.close()
                        })
                    }.observeOn(computation()).subscribeOn(computation()))
        } catch (error: ApplicationError) {
            SystemLog.get().error("TakeNotePresenterImpl", "Failed to save and close", error)
            return Single.just(Unit)
        }
    }

    override fun onCancel() {
        view.close(model.copyCredentials())
    }


}