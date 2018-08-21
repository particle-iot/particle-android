package io.particle.android.sdk.devicesetup.loaders;

import android.content.Context;

import java.io.IOException;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.utils.BetterAsyncTaskLoader;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.set;


/**
 * Returns the results of the "scan-ap" command from the device.
 * <p/>
 * Will return null if an exception is thrown when trying to send the command
 * and receive a reply from the device.
 */
@ParametersAreNonnullByDefault
public class ScanApCommandLoader extends BetterAsyncTaskLoader<Set<ScanAPCommandResult>> {

    private static final TLog log = TLog.get(ScanApCommandLoader.class);

    private final CommandClient commandClient;
    private final Set<ScanAPCommandResult> accumulatedResults = set();

    public ScanApCommandLoader(Context context, CommandClient client) {
        super(context);
        commandClient = client;
    }

    @Override
    public boolean hasContent() {
        return !accumulatedResults.isEmpty();
    }

    @Override
    public Set<ScanAPCommandResult> getLoadedContent() {
        return accumulatedResults;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    public Set<ScanAPCommandResult> loadInBackground() {
        try {
            ScanApCommand.Response response = commandClient.sendCommand(new ScanApCommand(),
                    ScanApCommand.Response.class);
            accumulatedResults.addAll(Funcy.transformList(response.getScans(), ScanAPCommandResult::new));
            log.d("Latest accumulated scan results: " + accumulatedResults);
            return set(accumulatedResults);

        } catch (IOException e) {
            log.e("Error running scan-ap command: ", e);
            return null;
        }
    }

}
