package io.particle.android.sdk.utils

import android.app.Activity
import android.widget.Toast

import javax.annotation.ParametersAreNonnullByDefault


@Deprecated(
    "Deprecated; will be removed in a future release",
    replaceWith = ReplaceWith("io.particle.mesh.setup.utils.safeToast()")
)
@ParametersAreNonnullByDefault
object Toaster {

    /**
     * Shows a toast message for a short time.
     *
     *
     * This is safe to call from background/worker threads.
     */
    @JvmStatic
    fun s(activity: Activity, msg: String?) {
        showToast(activity, msg, Toast.LENGTH_SHORT)
    }

    /**
     * Shows a toast message for a longer time than [.s].
     *
     *
     * This is safe to call from background/worker threads.
     */
    @JvmStatic
    fun l(activity: Activity, msg: String?) {
        showToast(activity, msg, Toast.LENGTH_LONG)
    }


    private fun showToast(
        activity: Activity, msg: String?,
        length: Int
    ) {
        Preconditions.checkNotNull(activity, "Activity must not be null!")

        val toastRunnable = { Toast.makeText(activity, msg, length).show() }

        if (EZ.isThisTheMainThread()) {
            toastRunnable()
        } else {
            EZ.runOnMainThread(toastRunnable)
        }
    }
}
