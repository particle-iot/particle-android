package io.particle.android.sdk.cloud;

import java.util.List;
import java.util.concurrent.CancellationException;

import io.particle.android.sdk.cloud.exceptions.PartialDeviceListResultException;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;

/**
 * A packaging hack to access the (temporarily) intentionally non-public API of
 * getDevicesParallel()
 */
public class ParallelDeviceFetcherAccessHack {

    public static List<ParticleDevice> getDevicesParallel(ParticleCloud cloud,
                                                          boolean useShortTimeouts)
            throws ParticleCloudException, PartialDeviceListResultException, CancellationException {
        return cloud.getDevicesParallel(useShortTimeouts);
    }

    public static List<ParticleDevice> getDeviceList(PartialDeviceListResultException ex) {
        return ex.devices;
    }

}
