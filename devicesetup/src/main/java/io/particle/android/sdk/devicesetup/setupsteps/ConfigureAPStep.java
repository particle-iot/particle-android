package io.particle.android.sdk.devicesetup.setupsteps;


import java.io.IOException;
import java.security.PublicKey;

import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ConfigureApCommand;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.utils.Crypto;


public class ConfigureAPStep extends SetupStep {

    private final CommandClient commandClient;
    private final SetupStepApReconnector workerThreadApConnector;
    private final ScanApCommand.Scan networkToConnectTo;
    private final String networkSecretPlaintext;
    private final PublicKey publicKey;

    private volatile boolean commandSent = false;

    ConfigureAPStep(StepConfig stepConfig, CommandClient commandClient,
                    SetupStepApReconnector workerThreadApConnector,
                    ScanApCommand.Scan networkToConnectTo, String networkSecretPlaintext,
                    PublicKey publicKey) {
        super(stepConfig);
        this.commandClient = commandClient;
        this.workerThreadApConnector = workerThreadApConnector;
        this.networkToConnectTo = networkToConnectTo;
        this.networkSecretPlaintext = networkSecretPlaintext;
        this.publicKey = publicKey;
    }

    protected void onRunStep() throws SetupStepException {
        WifiSecurity wifiSecurity = WifiSecurity.fromInteger(networkToConnectTo.wifiSecurityType);
        ConfigureApCommand.Builder builder = ConfigureApCommand.newBuilder()
                .setSsid(networkToConnectTo.ssid)
                .setSecurityType(wifiSecurity)
                .setChannel(networkToConnectTo.channel)
                .setIdx(0);
        if (wifiSecurity != WifiSecurity.OPEN) {
            try {
                builder.setEncryptedPasswordHex(
                        Crypto.encryptAndEncodeToHex(networkSecretPlaintext, publicKey));
            } catch (Crypto.CryptoException e) {
                // FIXME: try to throw a more specific exception here.
                // Don't throw SetupException here -- if this is failing, it's not
                // going to get any better by the running this SetupStep again, and
                // it can really only fail if the surrounding app code is doing something
                // wrong.  To wit: you *want* the app to crash here (or at least
                // throw out a dialog saying "horrible thing happened!  horrible error
                // code: ..." and then return to a safe "default" activity.
                throw new RuntimeException("Error encrypting network credentials", e);
            }
        }
        ConfigureApCommand command = builder.build();

        try {
            log.d("Ensuring connection to AP");
            workerThreadApConnector.ensureConnectionToSoftAp();

            ConfigureApCommand.Response response = commandClient.sendCommand(
                    command, ConfigureApCommand.Response.class);
            if (!response.isOk()) {
                throw new SetupStepException("Error response code " + response.responseCode +
                        " while configuring device");
            }
            log.d("Configure AP command returned: " + response.responseCode);
            commandSent = true;

        } catch (IOException e) {
            throw new SetupStepException(e);
        }
    }

    public boolean isStepFulfilled() {
        return commandSent;
    }

}
