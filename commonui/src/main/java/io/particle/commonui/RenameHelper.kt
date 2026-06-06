package io.particle.commonui


import android.text.InputType
import android.widget.EditText
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException
import io.particle.android.sdk.utils.Async
import org.greenrobot.eventbus.EventBus
import java.io.IOException


class RenameHelper private constructor(
    private val device: ParticleDevice,
    private val activity: FragmentActivity
) {

    companion object {

        @JvmStatic
        fun renameDevice(activity: FragmentActivity, device: ParticleDevice) {
            RenameHelper(device, activity).showDialog()
        }
    }

    private fun showDialog() {
        // FIXME: include "suggest different name" button
        // FIXME: device name cache is gone, re-implement this later.
        //        Set<String> noNames = Collections.emptySet();
        //        final String suggestedName = CoreNameGenerator.generateUniqueName(noNames);
        val suggestedName = device.name

        // FIXME: do validation here to prevent short names?
        // I think this is already done cloud-side.  Verify.
        val editText = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "Name your device"
            setText(suggestedName)
        }
        val pad = dpToPx(20, activity)
        val container = FrameLayout(activity).apply {
            setPadding(pad, 0, pad, 0)
            addView(editText)
        }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Name")
            .setView(container)
            .setPositiveButton("Save") { _, _ ->
                val onDupeName = Runnable { this@RenameHelper.showDialog() }
                rename(editText.text.toString(), onDupeName)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDupeNameDialog(runOnDupeName: Runnable?) {
        AlertDialog.Builder(activity)
            .setMessage("Sorry, you've already got a core by that name, try another one.")
            .setPositiveButton(android.R.string.ok) { _, _ ->
                runOnDupeName?.run()
            }
            .show()
    }

    private fun rename(newName: String, runOnDupeName: Runnable) {
        // FIXME: device name cache is gone, re-implement this later.
        //        Set<String> currentDeviceNames = device.getCloud().getDeviceNames();
        val currentDeviceNames = emptySet<String>()
        if (currentDeviceNames.contains(newName) || newName == device.name) {
            showDupeNameDialog(runOnDupeName)
        } else {
            doRename(newName)
        }
    }

    private fun doRename(newName: String) {
        try {
            Async.executeAsync(device, object : Async.ApiProcedure<ParticleDevice>() {
                @Throws(ParticleCloudException::class, IOException::class)
                override fun callApi(sparkDevice: ParticleDevice): Void? {
                    device.name = newName
                    EventBus.getDefault().post(device)
                    return null
                }

                override fun onFailure(exception: ParticleCloudException) {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle("Unable to rename core")
                        .setMessage(exception.bestMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
            }).andIgnoreCallbacksIfActivityIsFinishing(activity)
        } catch (e: ParticleCloudException) {
            MaterialAlertDialogBuilder(activity)
                .setTitle("Unable to rename core")
                .setMessage(e.bestMessage)
                .setPositiveButton("OK", null)
                .show()
        }

    }

}
