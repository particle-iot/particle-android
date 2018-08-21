package io.particle.android.sdk.devicesetup.ui;

import android.support.annotation.RestrictTo;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Locale;

import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.DeviceIdCommand;
import io.particle.android.sdk.devicesetup.commands.PublicKeyCommand;
import io.particle.android.sdk.devicesetup.commands.SetCommand;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepException;
import io.particle.android.sdk.utils.Crypto;
import io.particle.android.sdk.utils.ParticleDeviceSetupInternalStringUtils;
import io.particle.android.sdk.utils.TLog;

import static io.particle.android.sdk.utils.Py.truthy;

// FIXME: Even before it's done, I am pretty sure this will need
// to go through a round of "solve et coagula" before it's
// really right, at least maintenance-wise.
// FIXME: this naming is no longer really applicable.
public class DiscoverProcessWorker {
    private static final TLog log = TLog.get(DiscoverProcessWorker.class);

    private CommandClient client;

    private volatile String detectedDeviceID;

    volatile boolean isDetectedDeviceClaimed;
    volatile boolean gotOwnershipInfo;
    volatile boolean needToClaimDevice;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    DiscoverProcessWorker withClient(CommandClient client) {
        this.client = client;
        return this;
    }

    // FIXME: all this should probably become a list of commands to run in a queue,
    // each with shortcut conditions for when they've already been fulfilled, instead of
    // this if-else/try-catch ladder.
    public void doTheThing() throws SetupStepException {
        // 1. get device ID
        if (!truthy(detectedDeviceID)) {
            try {
                DeviceIdCommand.Response response = client.sendCommand(
                        new DeviceIdCommand(), DeviceIdCommand.Response.class);
                detectedDeviceID = response.deviceIdHex.toLowerCase(Locale.ROOT);
                DeviceSetupState.deviceToBeSetUpId = detectedDeviceID;
                isDetectedDeviceClaimed = truthy(response.isClaimed);
            } catch (IOException e) {
                throw new SetupStepException("Process died while trying to get the device ID", e);
            }
        }

        // 2. Get public key
        if (DeviceSetupState.publicKey == null) {
            try {
                DeviceSetupState.publicKey = getPublicKey();
            } catch (Crypto.CryptoException e) {
                throw new SetupStepException("Unable to get public key: ", e);

            } catch (IOException e) {
                throw new SetupStepException("Error while fetching public key: ", e);
            }
        }

        // 3. check ownership
        //
        // all cases:
        // (1) device not claimed `c=0` device should also not be in list from API => mobile
        //      app assumes user is claiming
        // (2) device claimed `c=1` and already in list from API => mobile app does not ask
        //      user about taking ownership because device already belongs to this user
        // (3) device claimed `c=1` and NOT in the list from the API => mobile app asks whether
        //      use would like to take ownership
        if (!gotOwnershipInfo) {
            needToClaimDevice = false;

            // device was never claimed before - so we need to claim it anyways
            if (!isDetectedDeviceClaimed) {
                setClaimCode();
                needToClaimDevice = true;

            } else {
                boolean deviceClaimedByUser = false;
                for (String deviceId : DeviceSetupState.claimedDeviceIds) {
                    if (deviceId.equalsIgnoreCase(detectedDeviceID)) {
                        deviceClaimedByUser = true;
                        break;
                    }
                }
                gotOwnershipInfo = true;

                if (isDetectedDeviceClaimed && !deviceClaimedByUser) {
                    // This device is already claimed by someone else. Ask the user if we should
                    // change ownership to the current logged in user, and if so, set the claim code.

                    throw new DiscoverDeviceActivity.DeviceAlreadyClaimed("Device already claimed by another user");

                } else {
                    // Success: no exception thrown, this part of the process is complete.
                    // Let the caller continue on with the setup process.
                }
            }

        } else {
            if (needToClaimDevice) {
                setClaimCode();
            }
            // Success: no exception thrown, the part of the process is complete.  Let the caller
            // continue on with the setup process.
        }
    }

    private void setClaimCode() throws SetupStepException {
        try {
            log.d("Setting claim code using code: " + DeviceSetupState.claimCode);

            String claimCodeNoBackslashes = ParticleDeviceSetupInternalStringUtils.remove(
                    DeviceSetupState.claimCode, "\\");
            SetCommand.Response response = client.sendCommand(
                    new SetCommand("cc", claimCodeNoBackslashes), SetCommand.Response.class);

            if (truthy(response.responseCode)) {
                // a non-zero response indicates an error, ala UNIX return codes
                throw new SetupStepException("Received non-zero return code from set command: "
                        + response.responseCode);
            }

            log.d("Successfully set claim code");

        } catch (IOException e) {
            throw new SetupStepException(e);
        }
    }

    private PublicKey getPublicKey() throws Crypto.CryptoException, IOException {
        PublicKeyCommand.Response response = this.client.sendCommand(
                new PublicKeyCommand(), PublicKeyCommand.Response.class);
        return Crypto.readPublicKeyFromHexEncodedDerString(response.publicKey);
    }
}
