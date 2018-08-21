package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.annotations.SerializedName;

import io.particle.android.sdk.utils.Preconditions;


public class SetCommand extends Command {

    @SerializedName("k")
    public final String key;

    @SerializedName("v")
    public final String value;

    public SetCommand(String key, String value) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(value, "Value cannot be null");
        this.key = key;
        this.value = value;
    }

    @Override
    public String getCommandName() {
        return "set";
    }


    public static class Response {

        @SerializedName("r")
        public final int responseCode;

        public Response(int responseCode) {
            this.responseCode = responseCode;
        }
    }

}
