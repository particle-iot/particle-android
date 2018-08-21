package io.particle.android.sdk.devicesetup.setupsteps;

import java.io.IOException;

import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ConnectAPCommand;


public class ConnectDeviceToNetworkStep extends SetupStep {

    private final CommandClient commandClient;
    private final SetupStepApReconnector workerThreadApConnector;

    private volatile boolean commandSent = false;

    ConnectDeviceToNetworkStep(StepConfig stepConfig, CommandClient commandClient,
                                      SetupStepApReconnector workerThreadApConnector) {
        super(stepConfig);
        this.commandClient = commandClient;
        this.workerThreadApConnector = workerThreadApConnector;
    }

    @Override
    protected void onRunStep() throws SetupStepException {
        try {
            log.d("Ensuring connection to AP");
            workerThreadApConnector.ensureConnectionToSoftAp();

            log.d("Sending connect-ap command");
            ConnectAPCommand.Response response = commandClient.sendCommand(
                    // FIXME: is hard-coding zero here correct?  If so, document why
                    new ConnectAPCommand(0), ConnectAPCommand.Response.class);
            if (!response.isOK()) {
                throw new SetupStepException("ConnectAPCommand returned non-zero response code: " +
                        response.responseCode);
            }

            commandSent = true;

        } catch (IOException e) {
            throw new SetupStepException(e);
        }
    }

    @Override
    public boolean isStepFulfilled() {
        return commandSent;
    }

}
