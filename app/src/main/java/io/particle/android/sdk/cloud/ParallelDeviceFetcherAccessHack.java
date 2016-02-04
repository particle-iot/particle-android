package io.particle.android.sdk.cloud;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import io.particle.android.sdk.cloud.ParticleCloud.PartialDeviceListResultException;

/**
 * A packaging hack to access the (temporarily) intentionally non-public API of
 * getDevicesParallel()
 */
@ParametersAreNonnullByDefault
public class ParallelDeviceFetcherAccessHack {

    public static List<ParticleDevice> getDevicesParallel(ParticleCloud cloud,
                                                          boolean useShortTimeouts)
            throws ParticleCloudException, PartialDeviceListResultException {
        return cloud.getDevicesParallel(useShortTimeouts);
    }

    public static List<ParticleDevice> getDeviceList(PartialDeviceListResultException ex) {
        return ex.devices;
    }

}
