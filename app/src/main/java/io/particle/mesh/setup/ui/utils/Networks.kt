package io.particle.mesh.setup.ui.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo


fun Context.localDeviceHasInternetConnection(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
    return activeNetwork?.isConnectedOrConnecting == true
}