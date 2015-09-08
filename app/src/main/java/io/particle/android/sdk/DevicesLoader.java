package io.particle.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.collect.ImmutableList;

import java.util.List;

import io.particle.android.sdk.cloud.BroadcastContract;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.utils.BetterAsyncTaskLoader;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


public class DevicesLoader extends BetterAsyncTaskLoader<List<ParticleDevice>> {

    private final ParticleCloud cloud;
    private final LocalBroadcastManager broadcastManager;
    private final DevicesUpdatedReceiver devicesUpdatedReceiver;
    private volatile List<ParticleDevice> devices = list();

    public DevicesLoader(Context context) {
        super(context);
        cloud = ParticleCloud.get(context);
        broadcastManager = LocalBroadcastManager.getInstance(context);
        devicesUpdatedReceiver = new DevicesUpdatedReceiver();
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
        return truthy(devices);
    }

    @Override
    public List<ParticleDevice> getLoadedContent() {
        return ImmutableList.copyOf(devices);
    }

    @Override
    public List<ParticleDevice> loadInBackground() {
        try {
            devices = cloud.getDevices();
        } catch (ParticleCloudException e) {
            // FIXME: think more about error handling here
        }
        return getLoadedContent();
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
