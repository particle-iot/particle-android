package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.annotations.SerializedName;

public class PublicKeyCommand extends NoArgsCommand {

    @Override
    public String getCommandName() {
        return "public-key";
    }


    public static class Response {

        @SerializedName("r")
        public final int responseCode;

        // Hex-encoded public key, in DER format
        @SerializedName("b")
        public final String publicKey;

        public Response(int responseCode, String publicKey) {
            this.responseCode = responseCode;
            this.publicKey = publicKey;
        }
    }

}
