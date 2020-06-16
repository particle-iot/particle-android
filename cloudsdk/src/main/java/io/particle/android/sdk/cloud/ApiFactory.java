package io.particle.android.sdk.cloud;

import android.content.Context;
import android.util.Base64;

import androidx.annotation.StringRes;

import com.google.gson.Gson;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;

import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import okio.ByteString;
import retrofit.RequestInterceptor.RequestFacade;
import retrofit.RestAdapter;
import retrofit.RestAdapter.Log;
import retrofit.RestAdapter.LogLevel;
import retrofit.client.OkClient;
import retrofit.converter.GsonConverter;


/**
 * Constructs ParticleCloud instances
 */
@ParametersAreNonnullByDefault
public class ApiFactory {

    // both values are in seconds
    private static final int REGULAR_TIMEOUT = 35;


    public interface TokenGetterDelegate {

        String getTokenValue();

    }


    public interface OauthBasicAuthCredentialsProvider {

        String getClientId();

        String getClientSecret();
    }


    private final TokenGetterDelegate tokenDelegate;
    private final OkHttpClient normalTimeoutClient;
    private final OauthBasicAuthCredentialsProvider basicAuthCredentialsProvider;
    private final Gson gson;
    private final LogLevel httpLogLevel;
    private final HttpUrl apiBaseUri;

    ApiFactory(
            HttpUrl uri,
            LogLevel httpLogLevel,
            TokenGetterDelegate tokenGetterDelegate,
            OauthBasicAuthCredentialsProvider basicAuthProvider
    ) {
        this.tokenDelegate = tokenGetterDelegate;
        this.basicAuthCredentialsProvider = basicAuthProvider;
        this.gson = new Gson();
        this.apiBaseUri = uri;
        this.httpLogLevel = httpLogLevel;

        normalTimeoutClient = buildClientWithTimeout(REGULAR_TIMEOUT);
    }

    private static OkHttpClient buildClientWithTimeout(int timeoutInSeconds) {
        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(timeoutInSeconds, TimeUnit.SECONDS);
        client.setReadTimeout(timeoutInSeconds, TimeUnit.SECONDS);
        client.setWriteTimeout(timeoutInSeconds, TimeUnit.SECONDS);
        return client;
    }

    ApiDefs.CloudApi buildNewCloudApi() {
        RestAdapter restAdapter = buildCommonRestAdapterBuilder(gson, normalTimeoutClient)
                .setRequestInterceptor(request -> {
                    request.addHeader("Authorization", "Bearer " + tokenDelegate.getTokenValue());
                    addParticleToolsHeader(request);
                })
                .build();
        return restAdapter.create(ApiDefs.CloudApi.class);
    }

    ApiDefs.IdentityApi buildNewIdentityApi() {
        final String basicAuthValue = getBasicAuthValue();

        RestAdapter restAdapter = buildCommonRestAdapterBuilder(gson, normalTimeoutClient)
                .setRequestInterceptor(request -> {
                    request.addHeader("Authorization", basicAuthValue);
                    addParticleToolsHeader(request);
                })
                .build();
        return restAdapter.create(ApiDefs.IdentityApi.class);
    }

    HttpUrl getApiUri() {
        return apiBaseUri;
    }

    Gson getGsonInstance() {
        return gson;
    }

    private String getBasicAuthValue() {
        String authString = String.format("%s:%s",
                basicAuthCredentialsProvider.getClientId(),
                basicAuthCredentialsProvider.getClientSecret());
        ByteString authBytes = ByteString.of(authString.getBytes());
        return "Basic " + authBytes.base64();
    }

    private void addParticleToolsHeader(RequestFacade request) {
        request.addHeader("X-Particle-Tool", "android-cloud-sdk");
    }

    private RestAdapter.Builder buildCommonRestAdapterBuilder(Gson gson, OkHttpClient client) {
        return new RestAdapter.Builder()
                .setClient(new OkClient(client))
                .setConverter(new GsonConverter(gson))
                .setEndpoint(getApiUri().toString())
                .setLogLevel(httpLogLevel);
    }


    public static class ResourceValueBasicAuthCredentialsProvider
            implements OauthBasicAuthCredentialsProvider {

        private final String clientId;
        private final String clientSecret;

        public ResourceValueBasicAuthCredentialsProvider(
                Context ctx, @StringRes int clientIdResId, @StringRes int clientSecretResId) {
            this.clientId = ctx.getString(clientIdResId);
            this.clientSecret = ctx.getString(clientSecretResId);
        }


        @Override
        public String getClientId() {
            return clientId;
        }

        @Override
        public String getClientSecret() {
            return clientSecret;
        }
    }

}
