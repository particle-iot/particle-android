package io.particle.android.sdk.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEventVisibility;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.cloud.models.DeviceStateChange;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

/**
 * An activity representing the Inspector screen for a Device.
 */
public class InspectorActivity extends BaseActivity {
    public static final String EXTRA_DEVICE = "EXTRA_DEVICE";

    public static Intent buildIntent(Context ctx, ParticleDevice device) {
        return new Intent(ctx, InspectorActivity.class)
                .putExtra(EXTRA_DEVICE, device);
    }

    private final Runnable syncStatus = new Runnable() {
        @Override
        public void run() {
            invalidateOptionsMenu();
            handler.postDelayed(syncStatus, 1000 * 60L);
        }
    };

    private ParticleDevice device;
    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector);

        // Show the Up button in the action bar.
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(getString(R.string.device_inspector));

        device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        TextView deviceNameView = Ui.findView(this, R.id.deviceName);
        deviceNameView.setText(device.getName());

        ImageView deviceStatus = Ui.findView(this, R.id.deviceStatus);
        Animation animFade = AnimationUtils.loadAnimation(this, R.anim.fade_in_out);
        deviceStatus.startAnimation(animFade);
        deviceStatus.setImageResource(getStatusColoredDot(device));

        setupInspectorPages();
        handler.postDelayed(syncStatus, 1000 * 60L);
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
        try {
            device.subscribeToSystemEvents();
        } catch (ParticleCloudException ignore) {
            //minor issue if we don't update online/offline states
        }
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        try {
            device.unsubscribeFromSystemEvents();
        } catch (ParticleCloudException ignore) {
        }
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                finish();
                return true;
            case R.id.action_event_publish:
                presentPublishDialog();
                return true;
            default:
                int actionId = item.getItemId();

                return DeviceActionsHelper.takeActionForDevice(actionId, this, device) ||
                        DeviceMenuUrlHandler.handleActionItem(this, actionId, item.getTitle()) ||
                        super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.inspector, menu);
        return true;
    }

    @Subscribe
    public void onEvent(ParticleDevice device) {
        //update device and UI
        //TODO update more fields
        this.device = device;
        TextView deviceNameView = Ui.findView(this, R.id.deviceName);
        deviceNameView.post(() -> deviceNameView.setText(device.getName()));
    }

    @Subscribe
    public void onEvent(DeviceStateChange deviceStateChange) {
        //reload menu to display online/offline
        invalidateOptionsMenu();
    }

    private int getStatusColoredDot(ParticleDevice device) {
        if (device.isFlashing()) {
            return R.drawable.device_flashing_dot;
        } else if (device.isConnected()) {
            if (device.isRunningTinker()) {
                return R.drawable.online_dot;

            } else {
                return R.drawable.online_non_tinker_dot;
            }

        } else {
            return R.drawable.offline_dot;
        }
    }

    private void setupInspectorPages() {
        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter(new InspectorPager(getSupportFragmentManager(), device));
        viewPager.setOffscreenPageLimit(3);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        //hiding keyboard on tab changed
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                View view = getCurrentFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (view != null && imm != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }


    private void presentPublishDialog() {
        final View publishDialogView = View.inflate(this, R.layout.publish_event, null);

        new AlertDialog.Builder(this,
                R.style.ParticleSetupTheme_DialogNoDimBackground)
                .setView(publishDialogView)
                .setPositiveButton(R.string.publish_positive_action, (dialog, which) -> {
                    TextView nameView = Ui.findView(publishDialogView, R.id.eventName);
                    TextView valueView = Ui.findView(publishDialogView, R.id.eventValue);
                    RadioButton privateEventRadio = Ui.findView(publishDialogView, R.id.privateEvent);

                    String name = nameView.getText().toString();
                    String value = valueView.getText().toString();
                    int eventVisibility = privateEventRadio.isChecked() ?
                            ParticleEventVisibility.PRIVATE : ParticleEventVisibility.PUBLIC;

                    publishEvent(name, value, eventVisibility);
                })
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .setOnCancelListener(DialogInterface::dismiss)
                .show();
    }

    private void publishEvent(String name, String value, int eventVisibility) {
        try {
            Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
                @Override
                public Void callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException {
                    particleDevice.getCloud().publishEvent(name, value, eventVisibility, 600);
                    return null;
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    Toast.makeText(InspectorActivity.this, "Failed to publish '" + name +
                            "' event", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (ParticleCloudException e) {
            Toast.makeText(this, "Failed to publish '" + name +
                    "' event", Toast.LENGTH_SHORT).show();
        }
    }
}