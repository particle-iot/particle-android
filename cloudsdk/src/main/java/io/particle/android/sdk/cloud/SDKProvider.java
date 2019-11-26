package io.particle.android.sdk.cloud

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.squareup.okhttp.HttpUrl
import io.particle.android.sdk.cloud.ApiDefs.CloudApi
import io.particle.android.sdk.cloud.ApiDefs.IdentityApi
import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider
import io.particle.android.sdk.cloud.ApiFactory.ResourceValueBasicAuthCredentialsProvider
import io.particle.android.sdk.cloud.ApiFactory.TokenGetterDelegate
import io.particle.android.sdk.cloud.R.string
import io.particle.android.sdk.cloud.SDKGlobals.appDataStorage
import io.particle.android.sdk.cloud.SDKGlobals.init
import io.particle.android.sdk.utils.BroadcastImpl
import java.util.concurrent.*
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicInteger
import javax.annotation.ParametersAreNonnullByDefault

// FIXME: there are a lot of details lacking in this class, but it's not public API, and the
// structure makes it easy enough to do something better later on.
@ParametersAreNonnullByDefault
internal class SDKProvider(
    context: Context,
    oAuthCredentialsProvider: OauthBasicAuthCredentialsProvider?,
    uri: HttpUrl?
) {
    private val ctx: Context
    val cloudApi: CloudApi
    val identityApi: IdentityApi
    val particleCloud: ParticleCloud
    private val tokenGetter: TokenGetterDelegateImpl

    private fun buildCloud(apiFactory: ApiFactory): ParticleCloud {
        init(ctx)
        // FIXME: see if this TokenGetterDelegate setter issue can be resolved reasonably
        val cloud = ParticleCloud(
            apiFactory.apiUri,
            cloudApi,
            identityApi,
            appDataStorage!!,
            BroadcastImpl(LocalBroadcastManager.getInstance(ctx)),
            apiFactory.gsonInstance,
            buildExecutor()
        )
        // FIXME: gross circular dependency
        tokenGetter.cloud = cloud
        return cloud
    }

    private class TokenGetterDelegateImpl : TokenGetterDelegate {
        @Volatile
        val cloud: ParticleCloud? = null

        override fun getTokenValue(): String {
            return cloud!!.accessToken!!
        }
    }

    companion object {
        private fun buildExecutor(): ExecutorService { // lifted from AsyncTask's executor config
            val CPU_COUNT = Runtime.getRuntime().availableProcessors()
            val CORE_POOL_SIZE = CPU_COUNT + 1
            val MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1
            val KEEP_ALIVE = 1
            // FIXME: how big should this queue be?
            val poolWorkQueue: BlockingQueue<Runnable> =
                LinkedBlockingQueue(1024)
            val threadFactory: ThreadFactory =
                object : ThreadFactory {
                    private val mCount =
                        AtomicInteger(1)

                    override fun newThread(r: Runnable): Thread {
                        return Thread(r, "Particle Exec #" + mCount.getAndIncrement())
                    }
                }
            return ThreadPoolExecutor(
                CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
                SECONDS, poolWorkQueue, threadFactory
            )
        }
    }

    init {
        var oAuthCredentialsProvider = oAuthCredentialsProvider
        ctx = context.applicationContext
        if (oAuthCredentialsProvider == null) {
            oAuthCredentialsProvider = ResourceValueBasicAuthCredentialsProvider(
                ctx, string.oauth_client_id, string.oauth_client_secret
            )
        }
        tokenGetter = TokenGetterDelegateImpl()
        val apiFactory = ApiFactory(ctx, tokenGetter, oAuthCredentialsProvider, uri)
        cloudApi = apiFactory.buildNewCloudApi()
        identityApi = apiFactory.buildNewIdentityApi()
        particleCloud = buildCloud(apiFactory)
    }
}