package org.dhis2.usescases.teiDashboard.dashboardsfragments.feedback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class FeedbackState {
    object Loading : FeedbackState()
    data class Loaded(val feedbackProgram: FeedbackProgram) : FeedbackState()
    data class ConfigurationError(val programUid: String) : FeedbackState()
    object UnexpectedError : FeedbackState()
}

class FeedbackPresenter(private val feedbackProgramRepository: FeedbackProgramRepository) :
    CoroutineScope by MainScope() {

    private var view: FeedbackView? = null

    fun attach(view: FeedbackView, uid: String) {
        this.view = view

        loadProgram(uid);
    }

    fun detach() {
        this.view = null
        cancel()
    }

    private fun loadProgram(uid: String) = launch() {
        render(FeedbackState.Loading)

        val result = withContext(Dispatchers.IO) { feedbackProgramRepository.get(uid) }

        result.fold(
            { failure -> handleFailure(failure) },
            { feedbackProgram ->
                render(
                    FeedbackState.Loaded(
                        feedbackProgram
                    )
                )
            })
    }

    private fun handleFailure(failure: FeedbackProgramFailure) {
        when (failure) {
            is FeedbackProgramFailure.ConfigurationError -> {
                render(
                    FeedbackState.ConfigurationError(failure.programUid)
                )
            }
            is FeedbackProgramFailure.UnexpectedError -> {
                render(FeedbackState.UnexpectedError)
            }
        }
    }

    private fun render(state: FeedbackState) {
        view?.render(state);
    }

    interface FeedbackView {
        fun render(state: FeedbackState)
    }
}