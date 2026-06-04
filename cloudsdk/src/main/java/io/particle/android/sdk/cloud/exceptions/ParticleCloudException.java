package io.particle.android.sdk.cloud.exceptions;


import androidx.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ParticleHttpError;
import io.particle.android.sdk.utils.ParticleInternalStringUtilsKt;

import static io.particle.android.sdk.utils.Py.list;


// Heavily inspired by Retrofit's old RetrofitError, which we used to wrap here.  We make our own
// checked exception both to avoid tying the public API to the HTTP library used by the
// implementation, and to keep the same shape as before now that the low-level error type is
// {@link ParticleHttpError}.
@ParametersAreNonnullByDefault
public class ParticleCloudException extends Exception {

    /**
     * Identifies the event kind which triggered a {@link ParticleCloudException}.
     */
    public enum Kind {

        /**
         * An {@link java.io.IOException} occurred while communicating to the server.
         */
        NETWORK,

        /**
         * An exception was thrown while (de)serializing a body.
         */
        CONVERSION,

        /**
         * A non-200 HTTP status code was received from the server.
         */
        HTTP,

        /**
         * An internal error occurred while attempting to execute a request.  It is best practice to
         * re-throw this exception so your application intentionally crashes.
         */
        UNEXPECTED
    }


    public static class ResponseErrorData {

        private final int httpStatusCode;
        @Nullable private final String body;

        ResponseErrorData(int httpStatusCode, @Nullable String body) {
            this.httpStatusCode = httpStatusCode;
            this.body = body;
        }

        public int getHttpStatusCode() {
            return httpStatusCode;
        }

        /**
         * @return response body as a String, or null if no body was returned.
         */
        @Nullable
        public String getBody() {
            return body;
        }

    }

    @Nullable final ResponseErrorData responseData;

    private final ParticleHttpError innerError;
    private boolean checkedForServerErrorMsg = false;
    private String serverErrorMessage;

    public ParticleCloudException(Exception exception) {
        super(exception);

        if (exception instanceof ParticleHttpError) {
            this.innerError = (ParticleHttpError) exception;
            this.responseData = buildResponseData(innerError);
        } else {
            // FIXME: ugly hack to get around even uglier bug.
            this.innerError = ParticleHttpError.unexpected(exception);
            this.responseData = null;
        }
    }

    /**
     * Response containing HTTP status code & body.
     * <p>
     * May be null depending on the nature of the error.
     */
    @Nullable
    public ResponseErrorData getResponseData() {
        return responseData;
    }

    /**
     * The event kind which triggered this error.
     */
    public Kind getKind() {
        return Kind.valueOf(innerError.getKind().toString());
    }

    /**
     * Any server-provided error message.  May be null.
     * <p>
     * If the server sent multiple errors, they will be concatenated together with newline characters.
     *
     * @return server-provided error or null
     */
    public String getServerErrorMsg() {
        if (!checkedForServerErrorMsg) {
            checkedForServerErrorMsg = true;
            serverErrorMessage = loadServerErrorMsg();
        }
        return serverErrorMessage;
    }

    /**
     * Returns a server provided message, if found, else just returns the result of the inner
     * exception's .getMessage()
     */
    public String getBestMessage() {
        // FIXME: this isn't the right place for user-facing data
        if (getKind() == Kind.NETWORK) {
            return "Unable to connect to the server.";

        } else if (getKind() == Kind.UNEXPECTED) {
            return "Unknown error communicating with server.";
        }
        String serverMsg = getServerErrorMsg();
        return (serverMsg == null) ? getMessage() : serverMsg;
    }

    private String loadServerErrorMsg() {
        if (responseData == null || responseData.getBody() == null) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(responseData.getBody());

            if (jsonObject.has("error_description")) {
                return jsonObject.getString("error_description");

            } else if (jsonObject.has("errors")) {
                List<String> errors = getErrors(jsonObject);
                return errors.isEmpty() ? null : ParticleInternalStringUtilsKt.join(errors, '\n');

            } else if (jsonObject.has("error")) {
                return jsonObject.getString("error");
            }

        } catch (JSONException ignore) {
        }
        return null;
    }

    private List<String> getErrors(JSONObject jsonObject) throws JSONException {
        List<String> errors = list();
        JSONArray jsonArray = jsonObject.getJSONArray("errors");
        if (jsonArray == null || jsonArray.length() == 0) {
            return errors;
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            String msg;

            JSONObject msgObj = jsonArray.optJSONObject(i);
            if (msgObj != null) {
                msg = msgObj.getString("message");
            } else {
                msg = jsonArray.get(i).toString();
            }

            errors.add(msg);
        }

        return errors;
    }


    @Nullable
    private ResponseErrorData buildResponseData(ParticleHttpError error) {
        if (error.getHttpStatusCode() == null) {
            return null;
        }
        return new ResponseErrorData(error.getHttpStatusCode(), error.getBody());
    }

}
