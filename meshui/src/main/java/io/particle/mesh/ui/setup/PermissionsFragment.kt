package io.particle.mesh.ui.setup

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import io.particle.android.sdk.utils.appHasPermission
import io.particle.mesh.ui.R
import java.util.*

class PermissionsFragment : Fragment(), OnRequestPermissionsResultCallback {

    companion object {

        const val TAG = "FRAG_MESH_PERMISSIONS"


        fun <T> ensureAttached(callbacksActivity: T): PermissionsFragment
                where T : FragmentActivity, T : Client {
            var frag: PermissionsFragment? = get(callbacksActivity)
            if (frag == null) {
                frag = PermissionsFragment()
                callbacksActivity.supportFragmentManager.commit { add(frag, TAG) }
            }
            return frag
        }

        fun <T> get(callbacksActivity: T): PermissionsFragment?
                where T : FragmentActivity, T : Client {
            val supportFragMan = callbacksActivity.supportFragmentManager
            return supportFragMan.findFragmentByTag(TAG) as PermissionsFragment?
        }

        private val REQUEST_CODE = 128
    }


    interface Client {
        fun onUserAllowedPermission(permission: String)
        fun onUserDeniedPermission(permission: String)
    }


    fun ensurePermission(permission: String) {
        val aktivity = activity!!

        if (aktivity.appHasPermission(permission)) {
            return
        }

        val dialogBuilder = AlertDialog.Builder(aktivity).setCancelable(false)

        // FIXME: stop referring to these location permission-specific strings here,
        // make them part of the client interface
        if (shouldShowRequestPermissionRationale(permission)) {
            dialogBuilder.setTitle(R.string.mesh_location_permission_dialog_title)
                .setMessage(R.string.mesh_location_permission_dialog_text)
                .setPositiveButton(R.string.mesh_got_it) { dialog, _ ->
                    dialog.dismiss()
                    requestPermission(permission)
                }
        } else {
            // user has explicitly denied this permission to setup.
            // show a simple dialog and bail out.
            dialogBuilder.setTitle(R.string.mesh_location_permission_denied_dialog_title)
                .setMessage(R.string.mesh_location_permission_denied_dialog_text)
                .setPositiveButton("Settings") { dialog, _ ->
                    dialog.dismiss()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val pkgName = aktivity.applicationInfo.packageName
                    intent.data = Uri.parse("package:$pkgName")
                    startActivity(intent)
                }
                .setNegativeButton("Exit") { _, _ ->
                    val client = activity as Client?
                    client!!.onUserDeniedPermission(permission)
                }
        }

        dialogBuilder.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        val msg = "onRequestPermissionsResult($requestCode, ${Arrays.toString(permissions)}," +
                "${Arrays.toString(grantResults)})"
        Log.i(TAG, msg)

        if (requestCode != REQUEST_CODE) {
            Log.i(TAG, "Unrecognized request code: $requestCode")
        }

        // we only ever deal with one permission at a time, so we can always safely grab the first
        // member of this array.
        val permission = permissions[0]
        val client = activity!! as Client
        if (activity!!.appHasPermission(permissions[0])) {
            client.onUserAllowedPermission(permission)
        } else {
            client.onUserDeniedPermission(permission)
        }
    }

    private fun requestPermission(permission: String) {
        requestPermissions(arrayOf(permission), REQUEST_CODE)
    }

}
