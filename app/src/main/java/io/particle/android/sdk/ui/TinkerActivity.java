package io.particle.android.sdk.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.tinker.TinkerFragment;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

/**
 * An activity representing the Tinker screen for a Device. This
 * activity is only used on handset devices. On tablet-size devices,
 * Tinker is presented side-by-side with a list of devices
 * in a {@link DeviceListActivity}.
 * <p/>
 * This activity is mostly just a 'shell' activity containing nothing
 * more than a {@link TinkerFragment}.
 */
public class TinkerActivity extends BaseActivity {


    public static Intent buildIntent(Context ctx, ParticleDevice device) {
        return new Intent(ctx, TinkerActivity.class).putExtra(TinkerFragment.ARG_DEVICE, device);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_detail);
        ParticleDevice device = getIntent().getParcelableExtra(TinkerFragment.ARG_DEVICE);

        // Show the Up button in the action bar.
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            supportActionBar.setDisplayHomeAsUpEnabled(true);
        }
        setTitle(getString(R.string.tinker));

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.device_detail_container, TinkerFragment.newInstance(device))
                    .commit();
        }

        ImageView deviceStatus = Ui.findView(this, R.id.deviceStatus);
        Animation animFade = AnimationUtils.loadAnimation(this, R.anim.fade_in_out);
        deviceStatus.startAnimation(animFade);
        deviceStatus.setImageResource(getStatusColoredDot(device));
    }

    @Override
    public void onResume(){
        super.onResume();
        ParticleDevice device = getIntent().getParcelableExtra(TinkerFragment.ARG_DEVICE);

        TextView deviceNameView = Ui.findView(this, R.id.deviceName);
        deviceNameView.setText(device.getName());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
