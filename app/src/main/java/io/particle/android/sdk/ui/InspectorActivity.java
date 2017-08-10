package io.particle.android.sdk.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.models.DeviceStateChange;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.truthy;

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
            // FIXME: do this with a theme attr instead.
            Drawable background = ContextCompat.getDrawable(this, R.drawable.ic_triangy_toolbar_background);
            supportActionBar.setBackgroundDrawable(background);
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
        if (id == android.R.id.home) {
            finish();
            return true;
        } else {
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
        ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new InspectorPager(getSupportFragmentManager(), device));
        viewPager.setOffscreenPageLimit(3);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabLayout);
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
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
        });
    }
}
