package io.particle.android.sdk.cloud;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * A Retrofit 2 {@link CallAdapter.Factory} that executes calls synchronously and returns the
 * deserialized body directly (rather than a {@code Call<T>}), throwing {@link ParticleHttpError}
 * on failure.
 *
 * <p>This preserves the SDK's long-standing synchronous, throw-on-error calling convention from
 * the Retrofit 1 era, so the (many) call sites in {@code ParticleCloud}/{@code ParticleDevice}
 * keep working with minimal change. API methods that genuinely want an async {@code Call<T>}
 * are left to Retrofit's default adapter.
 */
class SynchronousCallAdapterFactory extends CallAdapter.Factory {

    static SynchronousCallAdapterFactory create() {
        return new SynchronousCallAdapterFactory();
    }

    @Override
    public CallAdapter<?, ?> get(@NonNull Type returnType, @NonNull Annotation[] annotations,
                                 @NonNull Retrofit retrofit) {
        // Let Retrofit's built-in adapter handle Call<T> (and anything it special-cases).
        if (getRawType(returnType) == Call.class) {
            return null;
        }

        final Type bodyType = returnType;
        return new CallAdapter<Object, Object>() {
            @Override
            public Type responseType() {
                return bodyType;
            }

            @Override
            public Object adapt(@NonNull Call<Object> call) {
                Response<Object> response;
                try {
                    response = call.execute();
                } catch (IOException e) {
                    throw ParticleHttpError.network(e, call.request().url().toString());
                }

                if (response.isSuccessful()) {
                    return response.body();
                }

                String errorBodyString = null;
                try (ResponseBody errorBody = response.errorBody()) {
                    if (errorBody != null) {
                        errorBodyString = errorBody.string();
                    }
                } catch (IOException ignored) {
                    // best-effort read of the error body
                }
                throw ParticleHttpError.http(
                        response.code(), errorBodyString, call.request().url().toString());
            }
        };
    }
}
