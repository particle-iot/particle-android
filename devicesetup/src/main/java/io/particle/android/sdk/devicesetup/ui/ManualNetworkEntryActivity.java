package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.CheckBox;

import java.util.Set;

import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.commands.CommandClientFactory;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.devicesetup.loaders.ScanApCommandLoader;
import io.particle.android.sdk.devicesetup.model.ScanAPCommandResult;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


public class ManualNetworkEntryActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Set<ScanAPCommandResult>> {


    public static Intent buildIntent(Context ctx, SSID softApSSID) {
        return new Intent(ctx, ManualNetworkEntryActivity.class)
                .putExtra(EXTRA_SOFT_AP, softApSSID);
    }


    private static final String EXTRA_SOFT_AP = "EXTRA_SOFT_AP";


    @Inject protected WifiFacade wifiFacade;
    @Inject protected CommandClientFactory commandClientFactory;
    private SSID softApSSID;
    protected Integer wifiSecurityType = WifiSecurity.WPA2_AES_PSK.asInt();

    @OnCheckedChanged(R2.id.network_requires_password)
    protected void onSecureCheckedChange(boolean isChecked) {
        if (isChecked) {
            SEGAnalytics.track("Device Setup: Selected secured network");
            wifiSecurityType = WifiSecurity.WPA2_AES_PSK.asInt();
        } else {
            SEGAnalytics.track("Device Setup: Selected open network");
            wifiSecurityType = WifiSecurity.OPEN.asInt();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        SEGAnalytics.screen("Device Setup: Manual network entry screen");
        softApSSID = getIntent().getParcelableExtra(EXTRA_SOFT_AP);

        setContentView(R.layout.activity_manual_network_entry);
        ButterKnife.bind(this);
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);
    }

    public void onConnectClicked(View view) {
        String ssid = Ui.getText(this, R.id.network_name, true);
        ScanApCommand.Scan scan = new ScanApCommand.Scan(ssid, wifiSecurityType, 0);

        CheckBox requiresPassword = Ui.findView(this, R.id.network_requires_password);
        if (requiresPassword.isChecked()) {
            startActivity(PasswordEntryActivity.buildIntent(this, softApSSID, scan));
        } else {
            startActivity(ConnectingActivity.buildIntent(this, softApSSID, scan));
        }
    }

    public void onCancelClicked(View view) {
        finish();
    }

    // FIXME: loader not currently used, see note in onLoadFinished()
    @Override
    public Loader<Set<ScanAPCommandResult>> onCreateLoader(int id, Bundle args) {
        return new ScanApCommandLoader(this, commandClientFactory.newClientUsingDefaultsForDevices(wifiFacade, softApSSID));
    }

    @Override
    public void onLoadFinished(Loader<Set<ScanAPCommandResult>> loader, Set<ScanAPCommandResult> data) {
        // FIXME: perform process described here?:
        // https://github.com/spark/mobile-sdk-ios/issues/56
    }

    @Override
    public void onLoaderReset(Loader<Set<ScanAPCommandResult>> loader) {
        // no-op
    }
}
