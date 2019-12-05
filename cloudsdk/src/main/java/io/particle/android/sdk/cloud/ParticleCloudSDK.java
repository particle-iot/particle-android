package io.particle.android.sdk.cloud;

import android.content.Context;

import androidx.annotation.Nullable;

import com.squareup.okhttp.HttpUrl;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ApiFactory.OauthBasicAuthCredentialsProvider;
import io.particle.android.sdk.utils.TLog;

/**
 * Entry point for the Particle Cloud SDK.
 */
@ParametersAreNonnullByDefault
public class ParticleCloudSDK {
    // NOTE: pay attention to the interface, try to ignore the implementation, it's going to change.

    /**
     * Initialize the cloud SDK.  Must be called somewhere in your Application.onCreate()
     *
     * (or anywhere else before your first Activity.onCreate() is called)
     */
    public static void init(Context ctx) {
        initWithInstanceCheck(ctx, null);
    }

    public static void initWithOauthCredentialsProvider(
            Context ctx, @Nullable OauthBasicAuthCredentialsProvider oauthProvider) {
        initWithInstanceCheck(ctx, oauthProvider);
    }

    public static ParticleCloud getCloud() {
        verifyInitCalled();
        return instance.sdkProvider.getParticleCloud();
    }

    static void initWithInstanceCheck(Context ctx,
                                      @Nullable OauthBasicAuthCredentialsProvider oauthProvider) {
        if (instance != null) {
            log.w("Calling ParticleCloudSDK.init() more than once does not re-initialize the SDK.");
            return;
        }

        String asString = ctx.getString(R.string.api_base_uri);
        doInit(ctx, oauthProvider, HttpUrl.parse(asString));
    }

    private static void doInit(Context ctx,
                               @Nullable OauthBasicAuthCredentialsProvider oauthProvider,
                               HttpUrl uri) {
        Context appContext = ctx.getApplicationContext();
        SDKProvider sdkProvider = new SDKProvider(appContext, oauthProvider, uri);
        instance = new ParticleCloudSDK(sdkProvider);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isInitialized() {
        return instance != null;
    }

    // FIXME: when switching this to Kotlin, find the best way to mark this as
    // usable only from Particle packages
    static void reinitializeWithNewBaseApiUri(Context ctx, HttpUrl newUri) {
        doInit(ctx, null, newUri);
    }

    static SDKProvider getSdkProvider() {
        verifyInitCalled();
        return instance.sdkProvider;
    }

    static void verifyInitCalled() {
        if (!isInitialized()) {
            throw new IllegalStateException("init not called before using the Particle SDK. "
            + "Are you calling ParticleCloudSDK.init() in your Application.onCreate()?");
        }
    }


    private static final TLog log = TLog.get(ParticleCloudSDK.class);

    private static ParticleCloudSDK instance;


    private final SDKProvider sdkProvider;

    private ParticleCloudSDK(SDKProvider sdkProvider) {
        this.sdkProvider = sdkProvider;
    }

}
