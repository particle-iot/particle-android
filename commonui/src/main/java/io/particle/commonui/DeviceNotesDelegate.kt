package io.particle.commonui

import android.text.InputType
import android.view.Gravity
import android.widget.EditText
import android.widget.FrameLayout
import androidx.annotation.MainThread
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.mesh.setup.flow.Scopes
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle.State
import androidx.lifecycle.MutableLiveData


class DeviceNotesDelegate private constructor(
    private val activity: FragmentActivity,
    private val device: ParticleDevice,
    private val scopes: Scopes,
    private val newNoteDataLD: MutableLiveData<String>
) {

    companion object {

        @JvmStatic
        @MainThread
        fun editDeviceNotes(
            activity: FragmentActivity,
            device: ParticleDevice,
            scopes: Scopes,
            newNoteDataLD: MutableLiveData<String>
        ) {
            DeviceNotesDelegate(activity, device, scopes, newNoteDataLD).showDialog()
        }
    }


    private fun showDialog() {
        val editText = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            hint = "Use this space to keep notes on this device"
            setText(if (device.notes.isNullOrBlank()) null else device.notes)
            gravity = Gravity.START or Gravity.TOP
            setSingleLine(false)
        }
        val pad = dpToPx(20, activity)
        val container = FrameLayout(activity).apply {
            setPadding(pad, 0, pad, 0)
            addView(editText)
        }
        editText.updateLayoutParams {
            height = dpToPx(250, activity)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Notes")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                updateDeviceNotes(editText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
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
        val msg = "An error occurred. Device notes not updated"
        Snackbar.make(myRoot, msg, Snackbar.LENGTH_SHORT).show()
    }

}