package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.annotations.SerializedName;


public class VersionCommand extends NoArgsCommand {

    @Override
    public String getCommandName() {
        return "version";
    }


    public static class Response {

        @SerializedName("v")
        public final int version;

        public Response(int version) {
            this.version = version;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "version=" + version +
                    '}';
        }
    }

}
