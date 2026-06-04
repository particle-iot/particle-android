package io.particle.android.sdk.cloud;

import androidx.annotation.Nullable;

import java.io.IOException;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Unchecked error thrown by the synchronous Retrofit 2 call adapter when an API call fails.
 *
 * <p>This intentionally mirrors the information the SDK previously relied on from
 * Retrofit 1's {@code RetrofitError} (kind, HTTP status code, response body) so that
 * {@link io.particle.android.sdk.cloud.exceptions.ParticleCloudException} can keep wrapping
 * a single low-level error type without coupling the public API to the HTTP library.
 */
@ParametersAreNonnullByDefault
public class ParticleHttpError extends RuntimeException {

    public enum Kind {
        /** An {@link IOException} occurred while communicating with the server. */
        NETWORK,
        /** An exception was thrown while (de)serializing a body. */
        CONVERSION,
        /** A non-2xx HTTP status code was received from the server. */
        HTTP,
        /** An internal error occurred while attempting to execute a request. */
        UNEXPECTED
    }

    private final Kind kind;
    @Nullable private final Integer httpStatusCode;
    @Nullable private final String body;
    @Nullable private final String url;

    private ParticleHttpError(@Nullable String message, @Nullable Throwable cause, Kind kind,
                              @Nullable Integer httpStatusCode, @Nullable String body,
                              @Nullable String url) {
        super(message, cause);
        this.kind = kind;
        this.httpStatusCode = httpStatusCode;
        this.body = body;
        this.url = url;
    }

    public static ParticleHttpError http(int httpStatusCode, @Nullable String body, @Nullable String url) {
        return new ParticleHttpError("HTTP " + httpStatusCode + " " + url, null,
                Kind.HTTP, httpStatusCode, body, url);
    }

    public static ParticleHttpError network(IOException cause, @Nullable String url) {
        return new ParticleHttpError(cause.getMessage(), cause, Kind.NETWORK, null, null, url);
    }

    public static ParticleHttpError conversion(Throwable cause, @Nullable String url) {
        return new ParticleHttpError(cause.getMessage(), cause, Kind.CONVERSION, null, null, url);
    }

    public static ParticleHttpError unexpected(Throwable cause) {
        return new ParticleHttpError(cause.getMessage(), cause, Kind.UNEXPECTED, null, null, null);
    }

    public Kind getKind() {
        return kind;
    }

    /** HTTP status code, or null when the error was not an HTTP error. */
    @Nullable
    public Integer getHttpStatusCode() {
        return httpStatusCode;
    }

    /** Response body as a String, or null if none was available. */
    @Nullable
    public String getBody() {
        return body;
    }

    @Nullable
    public String getUrl() {
        return url;
    }
}
