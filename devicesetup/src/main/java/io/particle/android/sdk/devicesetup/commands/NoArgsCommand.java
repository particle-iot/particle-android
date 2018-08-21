package io.particle.android.sdk.devicesetup.commands;

import com.google.gson.Gson;

/**
 * Convenience class for commands with no argument data
 */
public abstract class NoArgsCommand extends Command {

    @Override
    public String argsAsJsonString(Gson gson) {
        // this command has no argument data
        return null;
    }
}
