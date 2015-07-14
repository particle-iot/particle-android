package io.particle.android.sdk;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.collect.ImmutableList;

import java.util.List;

import io.particle.android.sdk.cloud.BroadcastContract;
import io.particle.android.sdk.cloud.SparkCloud;
import io.particle.android.sdk.cloud.SparkCloudException;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.utils.BetterAsyncTaskLoader;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


public class DevicesLoader extends BetterAsyncTaskLoader<List<SparkDevice>> {

    private final SparkCloud cloud;
    private final LocalBroadcastManager broadcastManager;
    private final DevicesUpdatedReceiver devicesUpdatedReceiver;
    private volatile List<SparkDevice> devices = list();

    public DevicesLoader(Context context) {
        super(context);
        cloud = SparkCloud.get(context);
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
    public List<SparkDevice> getLoadedContent() {
        return ImmutableList.copyOf(devices);
    }

    @Override
    public List<SparkDevice> loadInBackground() {
        try {
            devices = cloud.getDevices();
            return getLoadedContent();
        } catch (SparkCloudException e) {
            // FIXME: think more about error handling here
            return getLoadedContent();
        }
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
