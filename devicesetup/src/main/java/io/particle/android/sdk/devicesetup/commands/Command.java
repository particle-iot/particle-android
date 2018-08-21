package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.Gson;


public abstract class Command {

    public abstract String getCommandName();

    // override if you want a different implementation
    public String argsAsJsonString(Gson gson) {
        return gson.toJson(this);
    }
}
