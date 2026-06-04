package io.particle.android.sdk.cloud;

import android.content.Context;

import androidx.annotation.StringRes;

import com.google.gson.Gson;

import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


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
    private final OauthBasicAuthCredentialsProvider basicAuthCredentialsProvider;
    private final Gson gson;
    private final HttpLoggingInterceptor.Level httpLogLevel;
    private final HttpUrl apiBaseUri;

    ApiFactory(
            HttpUrl uri,
            HttpLoggingInterceptor.Level httpLogLevel,
            TokenGetterDelegate tokenGetterDelegate,
            OauthBasicAuthCredentialsProvider basicAuthProvider
    ) {
        this.tokenDelegate = tokenGetterDelegate;
        this.basicAuthCredentialsProvider = basicAuthProvider;
        this.gson = new Gson();
        this.apiBaseUri = uri;
        this.httpLogLevel = httpLogLevel;
    }

    ApiDefs.CloudApi buildNewCloudApi() {
        OkHttpClient client = baseClientBuilder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("Authorization", "Bearer " + tokenDelegate.getTokenValue())
                                .build()))
                .build();
        return buildRetrofit(client).create(ApiDefs.CloudApi.class);
    }

    ApiDefs.IdentityApi buildNewIdentityApi() {
        final String basicAuthValue = getBasicAuthValue();
        OkHttpClient client = baseClientBuilder()
                .addInterceptor(chain -> chain.proceed(
                        chain.request().newBuilder()
                                .header("Authorization", basicAuthValue)
                                .build()))
                .build();
        return buildRetrofit(client).create(ApiDefs.IdentityApi.class);
    }

    HttpUrl getApiUri() {
        return apiBaseUri;
    }

    Gson getGsonInstance() {
        return gson;
    }

    private String getBasicAuthValue() {
        return Credentials.basic(
                basicAuthCredentialsProvider.getClientId(),
                basicAuthCredentialsProvider.getClientSecret());
    }

    private OkHttpClient.Builder baseClientBuilder() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(httpLogLevel);

        // Common "X-Particle-Tool" header added to every request.
        Interceptor toolsHeader = chain -> chain.proceed(
                chain.request().newBuilder()
                        .header("X-Particle-Tool", "android-cloud-sdk")
                        .build());

        return new OkHttpClient.Builder()
                .connectTimeout(REGULAR_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(REGULAR_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(REGULAR_TIMEOUT, TimeUnit.SECONDS)
                .addInterceptor(toolsHeader)
                .addInterceptor(logging);
    }

    private Retrofit buildRetrofit(OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(getApiUri())
                .client(client)
                .addCallAdapterFactory(SynchronousCallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
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
