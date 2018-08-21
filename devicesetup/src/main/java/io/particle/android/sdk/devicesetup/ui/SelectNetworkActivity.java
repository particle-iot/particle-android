package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.view.View;

import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.commands.CommandClient;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


public class SelectNetworkActivity extends RequiresWifiScansActivity
        implements WifiListFragment.Client<ScanAPCommandResult> {

    private static final String EXTRA_SOFT_AP = "EXTRA_SOFT_AP";


    public static Intent buildIntent(Context ctx, SSID deviceSoftAP) {
        return new Intent(ctx, SelectNetworkActivity.class)
                .putExtra(EXTRA_SOFT_AP, deviceSoftAP);
    }


    private WifiListFragment wifiListFragment;
    @Inject protected WifiFacade wifiFacade;
    @Inject protected CommandClientFactory commandClientFactory;
    private SSID softApSSID;

    @OnClick(R2.id.action_rescan)
    protected void onRescanClick() {
        ParticleUi.showParticleButtonProgress(SelectNetworkActivity.this, R.id.action_rescan, true);
        wifiListFragment.scanAsync();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        SEGAnalytics.screen("Device Setup: Select Network Screen");
        softApSSID = getIntent().getParcelableExtra(EXTRA_SOFT_AP);
        setContentView(R.layout.activity_select_network);
        ButterKnife.bind(this);

        wifiListFragment = Ui.findFrag(this, R.id.wifi_list_fragment);
    }

    public void onManualNetworkEntryClicked(View view) {
        startActivity(ManualNetworkEntryActivity.buildIntent(this, softApSSID));
        finish();
    }

    @Override
    public void onNetworkSelected(ScanAPCommandResult selectedNetwork) {
        if (WifiSecurity.isEnterpriseNetwork(selectedNetwork.scan.wifiSecurityType)) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.enterprise_networks_not_supported))
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
            return;
        }
        wifiListFragment.stopAggroLoading();

        if (selectedNetwork.isSecured()) {
            SEGAnalytics.track("Device Setup: Selected secured network");
            startActivity(PasswordEntryActivity.buildIntent(this, softApSSID, selectedNetwork.scan));
        } else {
            SEGAnalytics.track("Device Setup: Selected open network");
            SSID softApSSID = wifiFacade.getCurrentlyConnectedSSID();
            startActivity(ConnectingActivity.buildIntent(this, softApSSID, selectedNetwork.scan));
        }
        finish();
    }

    @Override
    public Loader<Set<ScanAPCommandResult>> createLoader(int id, Bundle args) {
        // FIXME: make the address below use resources instead of hardcoding
        CommandClient client = commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, softApSSID);
        return new ScanApCommandLoader(this, client);
    }

    @Override
    public void onLoadFinished() {
        ParticleUi.showParticleButtonProgress(this, R.id.action_rescan, false);
    }

    @Override
    public String getListEmptyText() {
        return getString(R.string.no_wifi_networks_found);
    }

    @Override
    public int getAggroLoadingTimeMillis() {
        return 10000;
    }

}
