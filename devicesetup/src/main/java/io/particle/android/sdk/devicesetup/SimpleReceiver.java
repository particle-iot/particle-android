package io.particle.android.sdk.devicesetup;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;


public class SimpleReceiver extends BroadcastReceiver {

    public interface LambdafiableBroadcastReceiver {
        void onReceive(Context context, Intent intent);
    }


    public static SimpleReceiver newReceiver(Context ctx, IntentFilter intentFilter,
                                             LambdafiableBroadcastReceiver receiver) {
        return new SimpleReceiver(ctx, intentFilter, receiver);
    }


    public static SimpleReceiver newRegisteredReceiver(Context ctx, IntentFilter intentFilter,
                                                       LambdafiableBroadcastReceiver receiver) {
        SimpleReceiver sr = new SimpleReceiver(ctx, intentFilter, receiver);
        sr.register();
        return sr;
    }


    private final Context appContext;
    private final IntentFilter intentFilter;
    private final LambdafiableBroadcastReceiver receiver;

    private boolean registered = false;

    private SimpleReceiver(Context ctx, IntentFilter intentFilter,
                           LambdafiableBroadcastReceiver receiver) {
        this.appContext = ctx.getApplicationContext();
        this.intentFilter = intentFilter;
        this.receiver = receiver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        receiver.onReceive(context, intent);
    }

    public void register() {
        if (registered) {
            return;
        }
        appContext.registerReceiver(this, intentFilter);
        registered = true;
    }

    public void unregister() {
        if (!registered) {
            return;
        }
        appContext.unregisterReceiver(this);
        registered = false;
    }

}
