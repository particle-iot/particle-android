package io.particle.android.sdk.devicesetup.ui;

import android.Manifest.permission;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;

import com.squareup.phrase.Phrase;

import java.util.Arrays;

import javax.inject.Inject;

import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.cloud.Responses.ClaimCodeResponse;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.di.ApModule;
import io.particle.android.sdk.ui.BaseActivity;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Async.AsyncApiWorker;
import io.particle.android.sdk.utils.SEGAnalytics;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.ParticleUi;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.android.sdk.utils.ui.WebViewActivity;

import static io.particle.android.sdk.utils.Py.truthy;


public class GetReadyActivity extends BaseActivity implements PermissionsFragment.Client {

    private static final TLog log = TLog.get(GetReadyActivity.class);

    @Inject protected ParticleCloud sparkCloud;
    @Inject protected SoftAPConfigRemover softAPConfigRemover;

    private AsyncApiWorker<ParticleCloud, ClaimCodeResponse> claimCodeWorker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_ready);
        ParticleDeviceSetupLibrary.getInstance().getApplicationComponent().activityComponentBuilder()
                .apModule(new ApModule()).build().inject(this);
        SEGAnalytics.screen("Device Setup: Get ready screen");
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        PermissionsFragment.ensureAttached(this);

        Ui.findView(this, R.id.action_im_ready).setOnClickListener(this::onReadyButtonClicked);
        Ui.setTextFromHtml(this, R.id.action_troubleshooting, R.string.troubleshooting)
                .setOnClickListener(v -> {
                    Uri uri = Uri.parse(v.getContext().getString(R.string.troubleshooting_uri));
                    startActivity(WebViewActivity.buildIntent(v.getContext(), uri));
                });

        Ui.setText(this, R.id.get_ready_text,
                Phrase.from(this, R.string.get_ready_text)
                        .put("device_name", getString(R.string.device_name))
                        .put("indicator_light_setup_color_name", getString(R.string.listen_mode_led_color_name))
                        .put("setup_button_identifier", getString(R.string.mode_button_name))
                        .format());

        Ui.setText(this, R.id.get_ready_text_title,
                Phrase.from(this, R.string.get_ready_title_text)
                        .put("device_name", getString(R.string.device_name))
                        .format());
    }

    @Override
    protected void onStart() {
        super.onStart();
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();

        if (sparkCloud.getAccessToken() == null && !BaseActivity.setupOnly) {
            startLoginActivity();
            finish();
        }
    }

    private void onReadyButtonClicked(View v) {
        if (claimCodeWorker != null && !claimCodeWorker.isCancelled()) {
            return;
        }
        DeviceSetupState.reset();
        if (BaseActivity.setupOnly) {
            moveToDeviceDiscovery();
            return;
        }
        showProgress(true);
        final Context ctx = this;
        claimCodeWorker = Async.executeAsync(sparkCloud, new Async.ApiWork<ParticleCloud, ClaimCodeResponse>() {
            @Override
            public ClaimCodeResponse callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException {
                return generateClaimCode(ctx);
            }

            @Override
            public void onTaskFinished() {
                claimCodeWorker = null;
                showProgress(false);
            }

            @Override
            public void onSuccess(@NonNull ClaimCodeResponse result) {
                handleClaimCode(result);
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException error) {
                onGenerateClaimCodeFail(error);
            }
        });
    }

    private void onGenerateClaimCodeFail(@NonNull ParticleCloudException error) {
        log.d("Generating claim code failed");
        ParticleCloudException.ResponseErrorData errorData = error.getResponseData();
        if (errorData != null && errorData.getHttpStatusCode() == 401) {
            onUnauthorizedError();
        } else {
            if (isFinishing()) {
                return;
            }

            // FIXME: we could just check the internet connection here ourselves...
            String errorMsg = getString(R.string.get_ready_could_not_connect_to_cloud);
            if (error.getMessage() != null) {
                errorMsg = errorMsg + "\n\n" + error.getMessage();
            }
            new AlertDialog.Builder(GetReadyActivity.this)
                    .setTitle(R.string.error)
                    .setMessage(errorMsg)
                    .setPositiveButton(R.string.ok, (dialog, which) -> dialog.dismiss())
                    .show();
        }
    }

    private void onUnauthorizedError() {
        if (isFinishing()) {
            sparkCloud.logOut();
            startLoginActivity();
            return;
        }

        String errorMsg = getString(R.string.get_ready_must_be_logged_in_as_customer,
                getString(R.string.brand_name));
        new AlertDialog.Builder(GetReadyActivity.this)
                .setTitle(R.string.access_denied)
                .setMessage(errorMsg)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    log.i("Logging out user");
                    sparkCloud.logOut();
                    startLoginActivity();
                    finish();
                })
                .show();
    }

    private void handleClaimCode(@NonNull ClaimCodeResponse result) {
        log.d("Claim code generated: " + result.claimCode);

        DeviceSetupState.claimCode = result.claimCode;
        if (truthy(result.deviceIds)) {
            DeviceSetupState.claimedDeviceIds.addAll(Arrays.asList(result.deviceIds));
        }

        if (isFinishing()) {
            return;
        }

        moveToDeviceDiscovery();
    }

    private ClaimCodeResponse generateClaimCode(Context ctx) throws ParticleCloudException {
        Resources res = ctx.getResources();
        if (res.getBoolean(R.bool.organization) && !res.getBoolean(R.bool.productMode)) {
            return sparkCloud.generateClaimCodeForOrg(res.getString(R.string.organization_slug),
                    res.getString(R.string.product_slug));
        } else if (res.getBoolean(R.bool.productMode)) {
            int productId = res.getInteger(R.integer.product_id);
            if (productId == 0) {
                throw new ParticleCloudException(new Exception("Product id must be set when productMode is in use."));
            }
            return sparkCloud.generateClaimCode(productId);
        } else {
            return sparkCloud.generateClaimCode();
        }
    }

    private void startLoginActivity() {
        startActivity(new Intent(this, LoginActivity.class));
    }

    private void showProgress(boolean show) {
        ParticleUi.showParticleButtonProgress(this, R.id.action_im_ready, show);
    }

    private void moveToDeviceDiscovery() {
        if (PermissionsFragment.hasPermission(this, permission.ACCESS_COARSE_LOCATION)) {
            startActivity(new Intent(GetReadyActivity.this, DiscoverDeviceActivity.class));
        } else {
            PermissionsFragment.get(this).ensurePermission(permission.ACCESS_COARSE_LOCATION);
        }
    }

    @Override
    public void onUserAllowedPermission(String permission) {
        moveToDeviceDiscovery();
    }

    @Override
    public void onUserDeniedPermission(String permission) {
        Toaster.s(this, getString(R.string.location_permission_denied_cannot_start_setup));
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        PermissionsFragment.get(this).onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
