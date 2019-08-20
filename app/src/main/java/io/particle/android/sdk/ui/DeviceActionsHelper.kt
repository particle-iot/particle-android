package io.particle.android.sdk.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.view.View
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.commonui.RenameHelper
import io.particle.mesh.setup.utils.safeToast
import io.particle.sdk.app.R


object DeviceActionsHelper {

    fun takeActionForDevice(
        actionId: Int,
        activity: FragmentActivity,
        device: ParticleDevice,
        view: View? = null
    ): Boolean {
        when (actionId) {
            R.id.action_device_rename -> {
                RenameHelper.renameDevice(activity, device)
            }

            R.id.action_device_unclaim -> {
                UnclaimHelper.unclaimDeviceWithDialog(activity, device)
            }

            R.id.action_device_inspector -> {
                activity.startActivity(InspectorActivity.buildIntent(activity, device))
            }

            R.id.action_device_flash_tinker -> {
                flashTinkerWithDialog(activity, view!!, device)
            }

            R.id.action_device_copy_id_to_clipboard -> {
                val clipboardMgr: ClipboardManager? = activity.getSystemService()
                clipboardMgr?.primaryClip = ClipData.newPlainText("simple text", device.id)
                // FIXME: use string rsrc
                activity.safeToast("Copied device ID to clipboard")
            }

            else -> return false
        }
        return true
    }

}
