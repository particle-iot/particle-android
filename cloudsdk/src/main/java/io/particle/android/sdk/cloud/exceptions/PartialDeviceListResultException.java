package io.particle.android.sdk.cloud.exceptions;

import java.util.List;

import io.particle.android.sdk.cloud.ParticleDevice;

public class PartialDeviceListResultException extends Exception {

    public final List<ParticleDevice> devices;

    public PartialDeviceListResultException(List<ParticleDevice> devices, Exception cause) {
        super(cause);
        this.devices = devices;
    }

    public PartialDeviceListResultException(List<ParticleDevice> devices) {
        super("Undefined error while fetching devices");
        this.devices = devices;
    }
}
