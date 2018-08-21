package io.particle.android.sdk.devicesetup.setupsteps;

import android.content.Context;
import android.support.annotation.RestrictTo;

import java.security.PublicKey;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.ui.SuccessActivity;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SetupStepsFactory {
    private static final int
            MAX_RETRIES_CONFIGURE_AP = 5,
            MAX_RETRIES_CONNECT_AP = 5,
            MAX_RETRIES_DISCONNECT_FROM_DEVICE = 5,
            MAX_RETRIES_CLAIM = 5;

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ConfigureAPStep newConfigureApStep(CommandClient commandClient, SetupStepApReconnector reconnector,
                                              ScanApCommand.Scan networkToConnectTo, String networkSecretPlaintext,
                                              PublicKey publicKey) {
        return new ConfigureAPStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CONFIGURE_AP)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.configure_device_wifi_credentials)
                        .build(),
                commandClient, reconnector, networkToConnectTo, networkSecretPlaintext, publicKey);
    }


    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public ConnectDeviceToNetworkStep newConnectDeviceToNetworkStep(CommandClient commandClient, SetupStepApReconnector reconnector) {
        return new ConnectDeviceToNetworkStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CONNECT_AP)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.connect_to_wifi_network)
                        .build(),
                commandClient, reconnector);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public WaitForDisconnectionFromDeviceStep newWaitForDisconnectionFromDeviceStep(SSID deviceSoftApSsid,
                                                                                    WifiFacade wifiFacade) {
        return new WaitForDisconnectionFromDeviceStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_NO_DISCONNECT)
                        .setStepId(R.id.reconnect_to_wifi_network)
                        .build(),
                deviceSoftApSsid, wifiFacade);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public EnsureSoftApNotVisible newEnsureSoftApNotVisible(SSID deviceSoftApSsid, WifiFacade wifiFacade) {
        return new EnsureSoftApNotVisible(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CONFIGURE)
                        .setStepId(R.id.wait_for_device_cloud_connection)
                        .build(),
                deviceSoftApSsid, wifiFacade);
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public WaitForCloudConnectivityStep newWaitForCloudConnectivityStep(Context context) {
        return new WaitForCloudConnectivityStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_DISCONNECT_FROM_DEVICE)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_NO_DISCONNECT)
                        .setStepId(R.id.check_for_internet_connectivity)
                        .build(), context.getApplicationContext());
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public CheckIfDeviceClaimedStep newCheckIfDeviceClaimedStep(ParticleCloud sparkCloud, String deviceId) {
        return new CheckIfDeviceClaimedStep(
                StepConfig.newBuilder()
                        .setMaxAttempts(MAX_RETRIES_CLAIM)
                        .setResultCode(SuccessActivity.RESULT_FAILURE_CLAIMING)
                        .setStepId(R.id.verify_product_ownership)
                        .build(),
                sparkCloud, deviceId);
    }
}
