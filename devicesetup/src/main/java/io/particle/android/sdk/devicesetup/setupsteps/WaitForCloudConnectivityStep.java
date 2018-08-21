package io.particle.android.sdk.devicesetup.setupsteps;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import io.particle.android.sdk.utils.EZ;


public class WaitForCloudConnectivityStep extends SetupStep {

    private static final int MAX_RETRIES_REACHABILITY = 1;

    private final Context ctx;

    WaitForCloudConnectivityStep(StepConfig stepConfig, Context ctx) {
        super(stepConfig);
        this.ctx = ctx;
    }

    @Override
    protected void onRunStep() throws SetupStepException {
        // Wait for just a couple seconds for a WifiFacade connection if possible, in case we
        // flip from the soft AP, to mobile data, and then to WifiFacade in rapid succession.
        EZ.threadSleep(2000);
        int reachabilityRetries = 0;
        boolean isAPIHostReachable = checkIsApiHostAvailable();
        while (!isAPIHostReachable && reachabilityRetries <= MAX_RETRIES_REACHABILITY) {
            EZ.threadSleep(2000);
            isAPIHostReachable = checkIsApiHostAvailable();
            log.d("Checked for reachability " + reachabilityRetries + " times");
            reachabilityRetries++;
        }
        if (!isAPIHostReachable) {
            throw new SetupStepException("Unable to reach API host");
        }
    }

    @Override
    public boolean isStepFulfilled() {
        return checkIsApiHostAvailable();
    }

    private boolean checkIsApiHostAvailable() {
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(
                Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = null;
        if (cm != null) {
            activeNetworkInfo = cm.getActiveNetworkInfo();
        }
        if (activeNetworkInfo == null || !activeNetworkInfo.isConnected()) {
            return false;
        }

        // FIXME: why is this commented out?  See what iOS does here now.
//        try {
//            cloud.getDevices();
//        } catch (Exception e) {
//            log.e("error checking availability: ", e);
//            // FIXME:
//            return false;
//            // At this stage we're technically OK with other types of errors
//            if (set(Kind.NETWORK, Kind.UNEXPECTED).contains(e.getKind())) {
//                return false;
//            }
//        }

        return true;
    }

}
