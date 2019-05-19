package io.particle.commonui

import android.text.InputType
import android.view.Gravity
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.Theme
import com.google.android.material.snackbar.Snackbar
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.mesh.setup.flow.Scopes
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.MutableLiveData


class DeviceNotesDelegate private constructor(
    private val activity: AppCompatActivity,
    private val device: ParticleDevice,
    private val scopes: Scopes,
    private val newNoteDataLD: MutableLiveData<String>
) {

    companion object {

        @JvmStatic
        @MainThread
        fun editDeviceNotes(
            activity: AppCompatActivity,
            device: ParticleDevice,
            scopes: Scopes,
            newNoteDataLD: MutableLiveData<String>
        ) {
            DeviceNotesDelegate(activity, device, scopes, newNoteDataLD).showDialog()
        }
    }


    private fun showDialog() {
        val md = MaterialDialog.Builder(activity)
            .title("Notes")
            .theme(Theme.LIGHT)
            .inputType(InputType.TYPE_CLASS_TEXT)
            .input("Use this space to keep notes on this device",
                if (device.notes.isNullOrBlank()) null else device.notes,
                false) { _, _ ->  }
            .positiveText("Save")
            .negativeText("Cancel")
            .onPositive(MaterialDialog.SingleButtonCallback { dialog, _ ->
                val inputText = dialog.inputEditText ?: return@SingleButtonCallback
                updateDeviceNotes(inputText.text.toString())
            })
            .show()
        md.inputEditText!!.updateLayoutParams {
            height = dpToPx(250, md.context)
        }
        md.inputEditText!!.gravity = Gravity.START or Gravity.TOP
        md.inputEditText!!.setSingleLine(false)
    }

    private fun updateDeviceNotes(newNotes: String?) {
        newNoteDataLD.postValue(newNotes)
        scopes.onWorker {
            try {
                device.notes = newNotes
            } catch (ex: Exception) {
                scopes.onMain {
                    showErrorSnackBar()
                }
            }
        }
    }

    private fun showErrorSnackBar() {
        if (!activity.lifecycle.currentState.isAtLeast(State.STARTED)) {
            return
        }

        val contentRoot = (activity.findViewById(android.R.id.content) as ViewGroup)
        val myRoot = contentRoot.getChildAt(0)
        Snackbar.make(myRoot, "An error occurred. Device notes not updated", Snackbar.LENGTH_SHORT)
    }

}