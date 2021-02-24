package io.particle.android.sdk.devicesetup.ui;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;

import com.squareup.phrase.Phrase;

import java.util.Set;

import javax.inject.Inject;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AlertDialog.Builder;
import androidx.loader.content.Loader;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.devicesetup.apconnector.ApConnector;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.loaders.WifiScanResultLoader;
import io.particle.android.sdk.devicesetup.model.ScanResultNetwork;
import io.particle.android.sdk.devicesetup.setupsteps.SetupStepException;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.truthy;


public class DiscoverDeviceActivity extends RequiresWifiScansActivity
        implements WifiListFragment.Client<ScanResultNetwork>, ApConnector.Client {


    // see ApConnector for the timeout value used for connecting to the soft AP
    private static final int MAX_NUM_DISCOVER_PROCESS_ATTEMPTS = 5;

    private static final TLog log = TLog.get(DiscoverDeviceActivity.class);


    @Inject protected WifiFacade wifiFacade;
    @Inject protected ParticleCloud sparkCloud;
    @Inject protected DiscoverProcessWorker discoverProcessWorker;
    @Inject protected SoftAPConfigRemover softAPConfigRemover;
    @Inject protected CommandClientFactory commandClientFactory;

    private WifiListFragment wifiListFragment;
    private ProgressDialog connectToApSpinnerDialog;

    private AsyncTask<Void, Void, SetupStepException> connectToApTask;
    private boolean isResumed = false;

    private int discoverProcessAttempts = 0;

    private SSID selectedSoftApSSID;

    @OnClick(R2.id.action_troubleshooting)
    protected void onTroubleshootingClick(View v) {
        Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
        startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
    }

    @OnClick(R2.id.action_log_out)
    protected void onLogoutClick() {
        sparkCloud.logOut();
        log.i("logged out, username is: " + sparkCloud.getLoggedInUsername());
        startActivity(new Intent(DiscoverDeviceActivity.this, LoginActivity.class));
        finish();
    }

    @OnClick(R2.id.action_cancel)
    protected void onCancelClick() {
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_discover_device);
        ButterKnife.bind(this);
        SEGAnalytics.screen("Device Setup: Device discovery screen");

        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        DeviceSetupState.previouslyConnectedWifiNetwork = wifiFacade.getCurrentlyConnectedSSID();

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment);
        ConnectToApFragment.ensureAttached(this);
        resetWorker();

        Ui.setText(this, R.id.wifi_list_header,
                Phrase.from(this, R.string.wifi_list_header_text)
                        .put("device_name", getString(R.string.device_name))
                        .format()
        );

        Ui.setText(this, R.id.msg_device_not_listed,
                Phrase.from(this, R.string.msg_device_not_listed)
                        .put("device_name", getString(R.string.device_name))
                        .put("setup_button_identifier", getString(R.string.mode_button_name))
                        .put("indicator_light", getString(R.string.indicator_light))
                        .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                        .format()
        );

        Ui.setTextFromHtml(this, R.id.action_troubleshooting, R.string.troubleshooting);

        if (!truthy(sparkCloud.getLoggedInUsername())) {
            Ui.findView(this, R.id.logged_in_as).setVisibility(View.GONE);
        } else {
            Ui.setText(this, R.id.logged_in_as,
                    Phrase.from(this, R.string.you_are_logged_in_as)
                            .put("username", sparkCloud.getLoggedInUsername())
                            .format()
            );
        }

        Ui.findView(this, R.id.action_log_out).setVisibility(BaseActivity.setupOnly ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!wifiFacade.isWifiEnabled()) {
            onWifiDisabled();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canGetLocation()) {
            onLocationDisabled();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isResumed = false;
    }

    private void resetWorker() {
        discoverProcessWorker.withClient(commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, selectedSoftApSSID));
    }

    private void onLocationDisabled() {
        log.d("Location disabled; prompting user");
        new Builder(this).setTitle(R.string.location_required)
                .setMessage(R.string.location_required_message)
                .setPositiveButton(R.string.enable_location, ((dialog, which) -> {
                    dialog.dismiss();
                    log.i("Sending user to enabling Location services.");
                    startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }))
                .setNegativeButton(R.string.exit_setup, ((dialog, which) -> {
                    dialog.dismiss();
                    finish();
                }))
                .show();
    }

    private boolean canGetLocation() {
        boolean gpsEnabled = false;
        boolean networkEnabled = false;
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            if (lm != null) {
                gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception ignored) {
        }
        return gpsEnabled || networkEnabled;
    }

    private void onWifiDisabled() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.Q) {
            // Wi-Fi disabled, but that's OK because on API 29+ devices, this case is handled
            // for us by the OS when we prompt to connect to an AP
            return;
        }

        log.d("Wi-Fi disabled; prompting user");
        new AlertDialog.Builder(this)
                .setTitle(R.string.wifi_required)
                .setPositiveButton(R.string.enable_wifi, (dialog, which) -> {
                    dialog.dismiss();
                    log.i("Enabling Wi-Fi at the user's request.");
                    wifiFacade.setWifiEnabled(true);
                    wifiListFragment.scanAsync();
                })
                .setNegativeButton(R.string.exit_setup, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    @Override
    public void onNetworkSelected(ScanResultNetwork selectedNetwork) {
        WifiConfiguration wifiConfig = ApConnector.buildUnsecuredConfig(selectedNetwork.getSsid());
        selectedSoftApSSID = selectedNetwork.getSsid();
        resetWorker();
        connectToSoftAp(wifiConfig);
    }

    private void connectToSoftAp(WifiConfiguration config) {
        discoverProcessAttempts++;
        softAPConfigRemover.onSoftApConfigured(SSID.from(config.SSID));
        ConnectToApFragment.get(this).connectToAP(config);
        showProgressDialog();
    }

    @Override
    public Loader<Set<ScanResultNetwork>> createLoader(int id, Bundle args) {
        return new WifiScanResultLoader(this, wifiFacade);
    }

    @Override
    public void onLoadFinished() {
        // no-op
    }

    @Override
    public String getListEmptyText() {
        return Phrase.from(this, R.string.empty_soft_ap_list_text)
                .put("device_name", getString(R.string.device_name))
                .format().toString();
    }

    @Override
    public int getAggroLoadingTimeMillis() {
        return 5000;
    }

    @Override
    public void onApConnectionSuccessful(WifiConfiguration config) {
        startConnectWorker();
    }

    @Override
    public void onApConnectionFailed(WifiConfiguration config) {
        hideProgressDialog();

        if (!canStartProcessAgain()) {
            onMaxAttemptsReached();
        } else {
            connectToSoftAp(config);
        }
    }

    private void showProgressDialog() {
        wifiListFragment.stopAggroLoading();

        String msg = Phrase.from(this, R.string.connecting_to_soft_ap)
                .put("device_name", getString(R.string.device_name))
                .format().toString();

        connectToApSpinnerDialog = new ProgressDialog(this);
        connectToApSpinnerDialog.setMessage(msg);
        connectToApSpinnerDialog.setCancelable(false);
        connectToApSpinnerDialog.setIndeterminate(true);
        connectToApSpinnerDialog.show();
    }

    private void hideProgressDialog() {
        wifiListFragment.startAggroLoading();
        if (connectToApSpinnerDialog != null) {
            if (!isFinishing()) {
                connectToApSpinnerDialog.dismiss();
            }
            connectToApSpinnerDialog = null;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void startConnectWorker() {
        // first, make sure we haven't actually been called twice...
        if (connectToApTask != null) {
            log.d("Already running connect worker " + connectToApTask + ", refusing to start another");
            return;
        }

        wifiListFragment.stopAggroLoading();
        if (!canStartProcessAgain()) {
            hideProgressDialog();
            onMaxAttemptsReached();
            return;
        }

        discoverProcessAttempts++;

        // This just has doInBackground() return null on success, or if an
        // exception was thrown, it passes that along instead to indicate failure.
        connectToApTask = new AsyncTask<Void, Void, SetupStepException>() {

            @Override
            protected SetupStepException doInBackground(Void... voids) {
                try {
                    // including this sleep because without it,
                    // we seem to attempt a socket connection too early,
                    // and it makes the process time out(!)
                    log.d("Waiting a couple seconds before trying the socket connection...");
                    EZ.threadSleep(2000);
                    discoverProcessWorker.doTheThing();
                    return null;

                } catch (SetupStepException e) {
                    log.d("Setup exception thrown: ", e);
                    return e;
                }
            }

            @Override
            protected void onPostExecute(SetupStepException error) {
                connectToApTask = null;
                if (error == null || (BaseActivity.setupOnly && error instanceof DeviceAlreadyClaimed)) {
                    // no exceptions thrown, huzzah
                    hideProgressDialog();
                    startActivity(SelectNetworkActivity.buildIntent(
                            DiscoverDeviceActivity.this, selectedSoftApSSID));
                    finish();
                } else if (error instanceof DeviceAlreadyClaimed) {
                    hideProgressDialog();
                    onDeviceClaimedByOtherUser();
                } else {
                    // nope, do it all over again.
                    startConnectWorker();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private boolean canStartProcessAgain() {
        return discoverProcessAttempts < MAX_NUM_DISCOVER_PROCESS_ATTEMPTS;
    }

    private void onMaxAttemptsReached() {
        if (!isResumed) {
            finish();
            return;
        }

        String errorMsg = Phrase.from(this, R.string.unable_to_connect_to_soft_ap)
                .put("device_name", getString(R.string.device_name))
                .format().toString();

        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(errorMsg)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    startActivity(new Intent(DiscoverDeviceActivity.this, GetReadyActivity.class));
                    finish();
                })
                .show();
    }

    private void onDeviceClaimedByOtherUser() {
        String dialogMsg = getString(R.string.dialog_title_owned_by_another_user,
                getString(R.string.device_name), sparkCloud.getLoggedInUsername());

        new Builder(this)
                .setTitle(getString(R.string.change_owner_question))
                .setMessage(dialogMsg)
                .setPositiveButton(getString(R.string.change_owner),
                        (dialog, which) -> {
                            dialog.dismiss();
                            log.i("Changing owner to " + sparkCloud.getLoggedInUsername());
                            resetWorker();
                            discoverProcessWorker.needToClaimDevice = true;
                            discoverProcessWorker.gotOwnershipInfo = true;
                            discoverProcessWorker.isDetectedDeviceClaimed = false;

                            showProgressDialog();
                            startConnectWorker();
                        })
                .setNegativeButton(R.string.cancel,
                        (dialog, which) -> {
                            dialog.dismiss();
                            startActivity(new Intent(DiscoverDeviceActivity.this, GetReadyActivity.class));
                            finish();
                        })
                .show();
    }

    static class DeviceAlreadyClaimed extends SetupStepException {

        DeviceAlreadyClaimed(String msg) {
            super(msg);
        }

    }

}
