package io.particle.android.sdk.ui.devicelist

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.squareup.phrase.Phrase
import io.particle.sdk.app.R


sealed class UserServiceAgreementsCheckResult {

    object SetupAllowed : UserServiceAgreementsCheckResult()

    data class LimitReached(val maxDevices: Int) : UserServiceAgreementsCheckResult()

    object NetworkError : UserServiceAgreementsCheckResult()
}


fun Fragment.showProgressDialog(): Dialog {
    @SuppressLint("InflateParams") // parent ViewGroups aren't used to inflate dialogs like this
    val dialogView = this.layoutInflater.inflate(R.layout.dialog_progress, null, false)
    return AlertDialog.Builder(requireActivity(), R.style.Theme_MaterialComponents_Dialog_Alert)
        .setView(dialogView)
        .setCancelable(true)
        .setOnCancelListener { it.dismiss() }
        .create()
        .apply {
            setCanceledOnTouchOutside(true)
            show()
        }
}


fun Fragment.showLimitReachedDialog(limit: Int) {
    val titleTmpl = R.string.TinkerStrings_DeviceList_Prompt_DeviceLimitReachedError_Title
    val title = Phrase.from(requireContext(), titleTmpl)
        .putOptional("limit", limit)
        .format()

    AlertDialog.Builder(requireActivity())
        .setTitle(title)
        .setMessage(R.string.TinkerStrings_DeviceList_Prompt_DeviceLimitReachedError_Message)
        .show()
}
