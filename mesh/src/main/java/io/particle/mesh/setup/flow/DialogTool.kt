package io.particle.mesh.setup.flow

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.particle.mesh.common.android.livedata.castAndPost
import io.particle.mesh.setup.ui.DialogResult
import io.particle.mesh.setup.ui.DialogSpec
import mu.KotlinLogging


// TODO: is this a hack, or is it just ideal for simple use cases?  Consider renaming.
class DialogTool(
    private val dialogRequestLD: LiveData<DialogSpec?>,
    val dialogResultLD: LiveData<DialogResult?>
) {

    private val log = KotlinLogging.logger {}


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

}