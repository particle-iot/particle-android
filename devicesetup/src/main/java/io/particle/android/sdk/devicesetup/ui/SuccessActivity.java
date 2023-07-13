package io.particle.android.sdk.devicesetup.ui;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.core.util.Pair;
import android.util.SparseArray;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.segment.analytics.Properties;
import com.squareup.phrase.Phrase;

import java.io.IOException;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.SDKGlobals;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteContract;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.R2;
import io.particle.android.sdk.devicesetup.SetupResult;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.ui.NextActivitySelector;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.WifiFacade;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.list;


public class SuccessActivity extends BaseActivity {

    public static final String EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE";
    public static final String EXTRA_DEVICE_ID = "EXTRA_DEVICE_ID";

    public static final int RESULT_SUCCESS = 1;
    public static final int RESULT_SUCCESS_UNKNOWN_OWNERSHIP = 2;
    public static final int RESULT_FAILURE_CLAIMING = 3;
    public static final int RESULT_FAILURE_CONFIGURE = 4;
    public static final int RESULT_FAILURE_NO_DISCONNECT = 5;
    public static final int RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE = 6;


    public static Intent buildIntent(Context ctx, int resultCode, String deviceId) {
        return new Intent(ctx, SuccessActivity.class)
                .putExtra(EXTRA_RESULT_CODE, resultCode)
                .putExtra(EXTRA_DEVICE_ID, deviceId);
    }

    private static final SparseArray<Pair<Integer, Integer>> resultCodesToStringIds;

    static {
        resultCodesToStringIds = new SparseArray<>(6);
        resultCodesToStringIds.put(RESULT_SUCCESS, Pair.create(
                R.string.setup_success_summary,
                R.string.setup_success_details));

        resultCodesToStringIds.put(RESULT_SUCCESS_UNKNOWN_OWNERSHIP, Pair.create(
                R.string.setup_success_unknown_ownership_summary,
                R.string.setup_success_unknown_ownership_details));

        resultCodesToStringIds.put(RESULT_FAILURE_CLAIMING, Pair.create(
                R.string.setup_failure_claiming_summary,
                R.string.setup_failure_claiming_details));

        resultCodesToStringIds.put(RESULT_FAILURE_CONFIGURE, Pair.create(
                R.string.setup_failure_configure_summary,
                R.string.setup_failure_configure_details));

        resultCodesToStringIds.put(RESULT_FAILURE_NO_DISCONNECT, Pair.create(
                R.string.setup_failure_no_disconnect_from_device_summary,
                R.string.setup_failure_no_disconnect_from_device_details));

        resultCodesToStringIds.put(RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE, Pair.create(
                R.string.setup_failure_configure_summary,
                R.string.setup_failure_lost_connection_to_device));
    }

    @BindView(R2.id.device_name) protected EditText deviceNameView;
    @BindView(R2.id.device_name_label) protected TextView deviceNameLabelView;
    @Inject protected ParticleCloud particleCloud;
    boolean isSuccess = false;

    @OnClick(R2.id.action_done)
    protected void onDoneClick(View v) {
        deviceNameView.setError(null);
        if (isSuccess && !BaseActivity.setupOnly) {
            if (deviceNameView.getVisibility() == View.VISIBLE && deviceNameView.getText().toString().isEmpty()) {
                deviceNameView.setError(getString(R.string.error_field_required));
            } else {
                finishSetup(v.getContext(), deviceNameView.getText().toString(), true);
            }
        } else {
            leaveActivity(v.getContext(), isSuccess);
        }
    }

    @OnClick(R2.id.action_troubleshooting)
    protected void onTroubleshootingClick(View v) {
        Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
        startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        ButterKnife.bind(this);
        SEGAnalytics.screen("Device Setup: Setup Result Screen");

        int resultCode = getIntent().getIntExtra(EXTRA_RESULT_CODE, -1);
        isSuccess = list(RESULT_SUCCESS, RESULT_SUCCESS_UNKNOWN_OWNERSHIP).contains(resultCode);

        SoftAPConfigRemover softAPConfigRemover = new SoftAPConfigRemover(
                this.getApplicationContext(),
                WifiFacade.get(this.getApplicationContext())
        );
        softAPConfigRemover.removeAllSoftApConfigs();

        if (!isSuccess) {
            ImageView image = Ui.findView(this, R.id.result_image);
            image.setImageResource(R.drawable.fail);
            deviceNameView.setVisibility(View.GONE);
            Properties analyticProperties = new Properties();

            switch (resultCode) {
                case RESULT_FAILURE_CLAIMING:
                    analyticProperties.putValue("reason", "claiming failed");
                    break;
                case RESULT_FAILURE_CONFIGURE:
                    analyticProperties.putValue("reason", "cannot configure");
                    break;
                case RESULT_FAILURE_NO_DISCONNECT:
                    analyticProperties.putValue("reason", "cannot disconnect");
                    break;
                case RESULT_FAILURE_LOST_CONNECTION_TO_DEVICE:
                    analyticProperties.putValue("reason", "lost connection");
                    break;
            }
            SEGAnalytics.track("Device Setup: Failure", analyticProperties);
        } else {
            showDeviceName(particleCloud);
            SEGAnalytics.track("Device Setup: Success", RESULT_SUCCESS_UNKNOWN_OWNERSHIP == resultCode ?
                    new Properties().putValue("reason", "not claimed") : null);
        }

        Pair<? extends CharSequence, CharSequence> resultStrings = buildUiStringPair(resultCode);
        Ui.setText(this, R.id.result_summary, resultStrings.first);
        Ui.setText(this, R.id.result_details, resultStrings.second);
        Ui.setTextFromHtml(this, R.id.action_troubleshooting, R.string.troubleshooting);
    }

    private void finishSetup(Context context, String deviceName, boolean isSuccess) {
        ParticleUi.showParticleButtonProgress(SuccessActivity.this, R.id.action_done, true);
        Async.executeAsync(particleCloud, new Async.ApiWork<ParticleCloud, Void>() {
            @Override
            public Void callApi(@NonNull ParticleCloud cloud) throws ParticleCloudException, IOException {
                ParticleDevice device = particleCloud.getDevice(getIntent().getStringExtra(EXTRA_DEVICE_ID));
                setDeviceName(device, deviceName);
                return null;
            }

            @Override
            public void onSuccess(@NonNull Void result) {
                leaveActivity(context, isSuccess);
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException e) {
                ParticleUi.showParticleButtonProgress(SuccessActivity.this, R.id.action_done, false);
                deviceNameView.setError(getString(R.string.device_naming_failure));
            }
        });
    }

    private void leaveActivity(Context context, boolean isSuccess) {
        Intent intent = NextActivitySelector.getNextActivityIntent(
                context,
                particleCloud,
                SDKGlobals.getSensitiveDataStorage(),
                new SetupResult(isSuccess, isSuccess ? DeviceSetupState.deviceToBeSetUpId : null));

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP
                | Intent.FLAG_ACTIVITY_CLEAR_TASK
                | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);

        Intent result = new Intent(DeviceSetupCompleteContract.ACTION_DEVICE_SETUP_COMPLETE)
                .putExtra(DeviceSetupCompleteContract.EXTRA_DEVICE_SETUP_WAS_SUCCESSFUL, isSuccess);
        if (isSuccess) {
            result.putExtra(DeviceSetupCompleteContract.EXTRA_CONFIGURED_DEVICE_ID,
                    DeviceSetupState.deviceToBeSetUpId);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(result);

        finish();
    }

    private void setDeviceName(ParticleDevice device, String deviceName) throws ParticleCloudException {
        //Set new device name only if it changed
        if (device.getName() != null && !device.getName().equals(deviceName)) {
            device.setName(deviceName);
        }
    }

    private void showDeviceName(ParticleCloud cloud) {
        Async.executeAsync(cloud, new Async.ApiWork<ParticleCloud, ParticleDevice>() {
            @Override
            public ParticleDevice callApi(@NonNull ParticleCloud cloud) throws ParticleCloudException, IOException {
                return particleCloud.getDevice(getIntent().getStringExtra(EXTRA_DEVICE_ID));
            }

            @Override
            public void onSuccess(@NonNull ParticleDevice particleDevice) {
                deviceNameLabelView.setVisibility(View.VISIBLE);
                deviceNameView.setVisibility(View.VISIBLE);
                deviceNameView.setText(particleDevice.getName());
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException e) {
                //In case setup was successful, but we cannot retrieve device naming would be a minor issue
                deviceNameView.setVisibility(View.GONE);
                deviceNameLabelView.setVisibility(View.GONE);
            }
        });
    }

    private Pair<? extends CharSequence, CharSequence> buildUiStringPair(int resultCode) {
        Pair<Integer, Integer> stringIds = resultCodesToStringIds.get(resultCode);
        return Pair.create(getString(stringIds.first),
                Phrase.from(this, stringIds.second)
                        .put("device_name", getString(R.string.device_name))
                        .format());
    }

}
