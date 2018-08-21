package io.particle.android.sdk.devicesetup.commands;

import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;

import static io.particle.android.sdk.devicesetup.commands.CommandClient.DEFAULT_TIMEOUT_SECONDS;

public class CommandClientFactory {

    public CommandClient newClient(WifiFacade wifiFacade, SSID softApSSID, String ipAddress, int port) {
        return new CommandClient(ipAddress, port,
                new NetworkBindingSocketFactory(wifiFacade, softApSSID, DEFAULT_TIMEOUT_SECONDS * 1000));
    }

    // FIXME: set these defaults in a resource file?
    public CommandClient newClientUsingDefaultsForDevices(WifiFacade wifiFacade, SSID softApSSID) {
        return newClient(wifiFacade, softApSSID, "192.168.0.1", 5609);
    }

}
