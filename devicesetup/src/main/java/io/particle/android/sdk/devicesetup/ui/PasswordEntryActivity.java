package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.gson.Gson;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.commands.ScanApCommand;
import io.particle.android.sdk.devicesetup.commands.data.WifiSecurity;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SSID;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;


// FIXME: password validation -- check for correct length based on security type?
// at least check for minimum.
public class PasswordEntryActivity extends BaseActivity {

    public static Intent buildIntent(Context ctx, SSID softApSSID,
                                     ScanApCommand.Scan networkToConnectTo) {
        return new Intent(ctx, PasswordEntryActivity.class)
                .putExtra(EXTRA_SOFT_AP_SSID, softApSSID)
                .putExtra(EXTRA_NETWORK_TO_CONFIGURE, ParticleDeviceSetupLibrary.getInstance()
                        .getApplicationComponent().getGson().toJson(networkToConnectTo));
    }


    private static final String
            EXTRA_NETWORK_TO_CONFIGURE = "EXTRA_NETWORK_TO_CONFIGURE",
            EXTRA_SOFT_AP_SSID = "EXTRA_SOFT_AP_SSID";


    private static final TLog log = TLog.get(PasswordEntryActivity.class);

    @BindView(R2.id.show_password) protected CheckBox showPwdBox;
    @BindView(R2.id.password) protected EditText passwordBox;
    private ScanApCommand.Scan networkToConnectTo;
    private SSID softApSSID;
    @Inject protected Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_entry);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this);
        SEGAnalytics.screen("Device Setup: Password Entry Screen");
        ParticleUi.enableBrandLogoInverseVisibilityAgainstSoftKeyboard(this);

        networkToConnectTo = gson.fromJson(
                getIntent().getStringExtra(EXTRA_NETWORK_TO_CONFIGURE),
                ScanApCommand.Scan.class);
        softApSSID = getIntent().getParcelableExtra(EXTRA_SOFT_AP_SSID);
        passwordBox.requestFocus();
        initViews();
    }

    private void initViews() {
        Ui.setText(this, R.id.ssid, networkToConnectTo.ssid);
        Ui.setText(this, R.id.security_msg, getSecurityTypeMsg());

        // set up onClick (et al) listeners
        showPwdBox.setOnCheckedChangeListener((buttonView, isChecked) -> togglePasswordVisibility(isChecked));
        // set up initial visibility state
        togglePasswordVisibility(showPwdBox.isChecked());
    }

    private void togglePasswordVisibility(boolean showPassword) {
        int inputType;
        if (showPassword) {
            inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD;
        } else {
            inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        }
        passwordBox.setInputType(inputType);
    }

    private String getSecurityTypeMsg() {
        WifiSecurity securityType = WifiSecurity.fromInteger(networkToConnectTo.wifiSecurityType);
        switch (securityType) {
            case WEP_SHARED:
            case WEP_PSK:
                return getString(R.string.secured_with_wep);
            case WPA_AES_PSK:
            case WPA_TKIP_PSK:
            case WPA_MIXED_PSK:
                return getString(R.string.secured_with_wpa);
            case WPA2_AES_PSK:
            case WPA2_MIXED_PSK:
            case WPA2_TKIP_PSK:
                return getString(R.string.secured_with_wpa2);
        }

        log.e("No security string found for " + securityType + "!");
        return "";
    }

    public void onCancelClicked(View view) {
        startActivity(SelectNetworkActivity.buildIntent(this, softApSSID));
        finish();
    }

    public void onConnectClicked(View view) {
        String secret = passwordBox.getText().toString().trim();
        startActivity(ConnectingActivity.buildIntent(
                this, softApSSID, networkToConnectTo, secret));
        finish();
    }
}
