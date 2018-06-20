package io.particle.particlemesh

import android.app.Application
import java.security.Security


class MeshApp : Application() {

    companion object {
        init {
            Security.insertProviderAt(org.spongycastle.jce.provider.BouncyCastleProvider(), 1)
        }
    }

    override fun onCreate() {
        super.onCreate()
    }

}