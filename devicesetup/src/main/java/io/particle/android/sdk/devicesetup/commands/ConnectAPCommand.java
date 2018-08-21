package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.annotations.SerializedName;

/**
 * Connects to an AP previously configured with configure-ap. This disconnects the soft-ap after
 * the response code has been sent. Note that the response code doesn't indicate successful
 * connection to the AP, but only that the command was acknowledged and the AP will be
 * connected to after the result is sent to the client.
 * <p/>
 * If the AP connection is unsuccessful, the soft-AP will be reinstated so the user can enter
 * new credentials/try again.
 */
public class ConnectAPCommand extends Command {

    @SerializedName("idx")
    public final int index;

    public ConnectAPCommand(int index) {
        this.index = index;
    }

    @Override
    public String getCommandName() {
        return "connect-ap";
    }


    public static class Response {

        @SerializedName("r")
        public final int responseCode;  // 0 == OK, non-zero == problem with index/data

        public Response(int responseCode) {
            this.responseCode = responseCode;
        }

        public boolean isOK() {
            return responseCode == 0;
        }

        @Override
        public String toString() {
            return "Response{" +
                    "responseCode=" + responseCode +
                    '}';
        }
    }
}
