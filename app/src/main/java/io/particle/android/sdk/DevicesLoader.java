package io.particle.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import androidx.annotation.WorkerThread;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import io.particle.android.sdk.cloud.BroadcastContract;
import io.particle.android.sdk.cloud.ParallelDeviceFetcherAccessHack;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.exceptions.PartialDeviceListResultException;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.BetterAsyncTaskLoader;

import static io.particle.android.sdk.utils.Py.truthy;


public class DevicesLoader extends BetterAsyncTaskLoader<DevicesLoader.DevicesLoadResult> {


    public static class DevicesLoadResult {

        public final List<ParticleDevice> devices;
        public final boolean isPartialResult;
        // FIXME: naming.  also, two booleans in a constructor, in a row.... no. just no.
        public final boolean unableToLoadAnyDevices;

        DevicesLoadResult(List<ParticleDevice> devices, boolean isPartialResult,
                          boolean unableToLoadAnyDevices) {
            this.devices = devices;
            this.isPartialResult = isPartialResult;
            this.unableToLoadAnyDevices = unableToLoadAnyDevices;
        }
    }


    private final ParticleCloud cloud;
    private final LocalBroadcastManager broadcastManager;
    private final DevicesUpdatedReceiver devicesUpdatedReceiver;

    private volatile DevicesLoadResult latestResult = new DevicesLoadResult(
            new ArrayList<>(), false, false);
    private volatile boolean useLongTimeoutsOnNextLoad = false;

    public DevicesLoader(Context context) {
        super(context);
        cloud = ParticleCloudSDK.getCloud();
        broadcastManager = LocalBroadcastManager.getInstance(context);
        devicesUpdatedReceiver = new DevicesUpdatedReceiver();
    }

    public void setUseLongTimeoutsOnNextLoad(boolean useLongTimeoutsOnNextLoad) {
        this.useLongTimeoutsOnNextLoad = useLongTimeoutsOnNextLoad;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        broadcastManager.registerReceiver(devicesUpdatedReceiver, devicesUpdatedReceiver.getFilter());
    }

    @Override
    protected void onStopLoading() {
        super.onStopLoading();
        broadcastManager.unregisterReceiver(devicesUpdatedReceiver);
    }

    @Override
    public boolean hasContent() {
        return truthy(latestResult.devices);
    }

    @Override
    public DevicesLoadResult getLoadedContent() {
        return latestResult;
    }

    @Override
    @WorkerThread
    public DevicesLoadResult loadInBackground() {
        boolean useLongTimeouts = useLongTimeoutsOnNextLoad;
        useLongTimeoutsOnNextLoad = false;

        List<ParticleDevice> devices;
        boolean isPartialList = false;
        boolean unableToLoadAnyDevices = false;
        try {
            devices = ParallelDeviceFetcherAccessHack.getDevicesParallel(cloud, useLongTimeouts);
        } catch (ParticleCloudException | CancellationException e) {
            // FIXME: think more about error handling here
            // getting a PCE here means we couldn't even get the device list, so just return
            // whatever we have.
            unableToLoadAnyDevices = true;
            devices = latestResult.devices;

        } catch (PartialDeviceListResultException ex) {
            ex.printStackTrace();
            devices = ParallelDeviceFetcherAccessHack.getDeviceList(ex);
            isPartialList = true;
        }

        DevicesLoadResult resultToReturn = new DevicesLoadResult(
                new ArrayList<>(devices), isPartialList, unableToLoadAnyDevices);

        if (!unableToLoadAnyDevices) {
            latestResult = resultToReturn;
        }

        return resultToReturn;
    }


    private class DevicesUpdatedReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            forceLoad();
        }

        IntentFilter getFilter() {
            return new IntentFilter(BroadcastContract.BROADCAST_DEVICES_UPDATED);
        }
    }

}
