package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.annotations.SerializedName;

/**
 * Retrieves the unique device ID as a 24-digit hex string
 */
public class DeviceIdCommand extends NoArgsCommand {


    @Override
    public String getCommandName() {
        return "device-id";
    }


    public static class Response {

        @SerializedName("id")
        public final String deviceIdHex;

        @SerializedName("c")
        public final int isClaimed;


        public Response(String deviceIdHex, int isClaimed) {
            this.deviceIdHex = deviceIdHex;
            this.isClaimed = isClaimed;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "deviceIdHex='" + deviceIdHex + '\'' +
                    ", isClaimed=" + isClaimed +
                    '}';
        }
    }

}
