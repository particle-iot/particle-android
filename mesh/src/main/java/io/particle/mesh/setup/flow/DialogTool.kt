package io.particle.mesh.setup.flow

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import io.particle.mesh.common.android.livedata.castAndPost
import mu.KotlinLogging


enum class DialogResult {
    POSITIVE,
    NEGATIVE,
}


sealed class DialogSpec {

    data class ResDialogSpec(
        @StringRes val text: Int,
        @StringRes val positiveText: Int,
        @StringRes val negativeText: Int? = null,
        @StringRes val title: Int? = null
    ) : DialogSpec()

    data class StringDialogSpec(
        val text: String
    ) : DialogSpec()

}



// TODO: is this a hack, or is it just ideal for simple use cases?  Consider renaming.
class DialogTool(
    private val dialogRequestLD: LiveData<DialogSpec?>,
    private val snackbarRequestLD: LiveData<String?>,
    val dialogResultLD: LiveData<DialogResult?>
) {

    private val log = KotlinLogging.logger {}


    fun newSnackbarRequest(messageToShow: String) {
        log.debug { "newSnackbarRequest(): $messageToShow" }
        snackbarRequestLD.castAndPost(messageToShow)
    }

    fun newDialogRequest(spec: DialogSpec) {
        log.debug { "newDialogRequest(): $spec" }
        dialogRequestLD.castAndPost(spec)
    }

    fun updateDialogResult(dialogResult: DialogResult) {
        log.debug { "updateDialogResult(): $dialogResult" }
        dialogResultLD.castAndPost(dialogResult)
    }

    fun clearDialogResult() {
        dialogResultLD.castAndPost(null)
    }

    fun clearDialogRequest() {
        dialogRequestLD.castAndPost(null)
    }

    fun clearSnackbarRequest() {
        snackbarRequestLD.castAndPost(null)
    }
}