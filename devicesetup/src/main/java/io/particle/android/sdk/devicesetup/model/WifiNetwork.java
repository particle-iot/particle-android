package io.particle.android.sdk.devicesetup.model;

import io.particle.android.sdk.utils.SSID;


public interface WifiNetwork {

    SSID getSsid();

    boolean isSecured();
}
