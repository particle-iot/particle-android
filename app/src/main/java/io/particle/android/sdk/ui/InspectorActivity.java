package io.particle.android.sdk.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import io.particle.android.sdk.cloud.ParticleDevice;
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
            ColorDrawable color = new ColorDrawable(ContextCompat.getColor(this, R.color.shaded_background));
            supportActionBar.setBackgroundDrawable(color);
        }

        device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        String name = truthy(device.getName()) ? device.getName() : "(Unnamed device)";
        setTitle(name);
        setupInspectorPages();
        handler.postDelayed(syncStatus, 1000 * 60L);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. Use NavUtils to allow users
            // to navigate up one level in the application structure. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            NavUtils.navigateUpTo(this, new Intent(this, DeviceListActivity.class));
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
        MenuItem statusItem = menu.findItem(R.id.action_online_status);
        statusItem.setIcon(getStatusColoredDot(device));
        return true;
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
    }
}
