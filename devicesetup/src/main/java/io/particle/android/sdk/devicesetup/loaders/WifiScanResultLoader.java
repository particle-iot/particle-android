package io.particle.android.sdk.devicesetup.loaders;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.SimpleReceiver;
import io.particle.android.sdk.devicesetup.model.ScanResultNetwork;
import io.particle.android.sdk.utils.BetterAsyncTaskLoader;
import io.particle.android.sdk.utils.Funcy;
import io.particle.android.sdk.utils.Funcy.Predicate;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;

import static io.particle.android.sdk.utils.Py.set;
import static io.particle.android.sdk.utils.Py.truthy;


public class WifiScanResultLoader extends BetterAsyncTaskLoader<Set<ScanResultNetwork>> {

    private static final TLog log = TLog.get(WifiScanResultLoader.class);


    private final WifiFacade wifiFacade;
    private final SimpleReceiver receiver;
    private volatile Set<ScanResultNetwork> mostRecentResult;
    private volatile int loadCount = 0;

    public WifiScanResultLoader(Context context, WifiFacade wifiFacade) {
        super(context);
        Context appCtx = context.getApplicationContext();
        receiver = SimpleReceiver.newReceiver(
                appCtx, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
                (ctx, intent) -> {
                    log.d("Received WifiManager.SCAN_RESULTS_AVAILABLE_ACTION broadcast");
                    forceLoad();
                });
        this.wifiFacade = wifiFacade;
    }

    @Override
    public boolean hasContent() {
        return mostRecentResult != null;
    }

    @Override
    public Set<ScanResultNetwork> getLoadedContent() {
        return mostRecentResult;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        receiver.register();
        forceLoad();
    }

    @Override
    protected void onStopLoading() {
        receiver.unregister();
        cancelLoad();
    }

    @Override
    public Set<ScanResultNetwork> loadInBackground() {
        List<ScanResult> scanResults = wifiFacade.getScanResults();
        log.d("Latest (unfiltered) scan results: " + scanResults);

        if (loadCount % 3 == 0) {
            wifiFacade.startScan();
        }

        loadCount++;
        // filter the list, transform the matched results, then wrap those in a Set
        mostRecentResult = set(Funcy.transformList(
                scanResults, ssidStartsWithProductName, ScanResultNetwork::new));

        if (mostRecentResult.isEmpty()) {
            log.i("No SSID scan results returned after filtering by product name.  " +
                    "Do you need to change the 'network_name_prefix' resource?");
        }

        return mostRecentResult;
    }


    private final Predicate<ScanResult> ssidStartsWithProductName = input -> {
        if (!truthy(input.SSID)) {
            return false;
        }
        String softApPrefix = (getContext().getString(R.string.network_name_prefix) + "-").toLowerCase(Locale.ROOT);
        return input.SSID.toLowerCase(Locale.ROOT).startsWith(softApPrefix);
    };

}
