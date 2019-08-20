package io.particle.android.sdk.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.PopupMenu

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.utils.ui.Toaster
import io.particle.commonui.RenameHelper
import io.particle.sdk.app.R

import io.particle.android.sdk.ui.flashPhotonTinkerWithDialog
import io.particle.android.sdk.ui.flashTinkerWithDialog


object DeviceActionsHelper {

    internal fun buildPopupMenuHelper(
        activity: FragmentActivity?, device: ParticleDevice
    ): PopupMenu.OnMenuItemClickListener {
        return { item -> takeActionForDevice(item.getItemId(), activity, device) }
    }


    fun buildPopupMenuHelper(
        fragment: Fragment,
        device: ParticleDevice
    ): PopupMenu.OnMenuItemClickListener {
        return buildPopupMenuHelper(fragment.activity, device)
    }


    fun takeActionForDevice(
        actionId: Int, activity: FragmentActivity?,
        device: ParticleDevice
    ): Boolean {
        when (actionId) {
            R.id.action_device_rename -> {
                RenameHelper.renameDevice(activity!!, device)
                return true
            }

            R.id.action_device_unclaim -> {
                UnclaimHelper.unclaimDeviceWithDialog(activity, device)
                return true
            }

            R.id.action_device_inspector -> {
                activity!!.startActivity(InspectorActivity.buildIntent(activity, device))
                return true
            }

            R.id.action_device_flash_tinker ->
                //                flashTinkerWithDialog();
                //                if (device.getDeviceType() == ParticleDevice.ParticleDeviceType.CORE) {
                //                    flashKnownAppWithDialog(activity, device, ParticleDevice.KnownApp.TINKER);
                //                } else {
                //                    flashPhotonTinkerWithDialog(activity, device);
                //                }
                return true

            R.id.action_device_copy_id_to_clipboard -> {
                val clipboardMgr: ClipboardManager
                clipboardMgr =
                    activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardMgr.primaryClip = ClipData.newPlainText("simple text", device.id)
                // FIXME: use string rsrc
                Toaster.s(activity, "Copied device ID to clipboard")
                return true
            }

            else -> return false
        }
    }

}
