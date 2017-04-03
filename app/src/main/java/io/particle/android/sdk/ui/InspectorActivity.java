package io.particle.android.sdk.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.tinker.TinkerFragment;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_inspector);

        // Show the Up button in the action bar.
        ActionBar supportActionBar = getSupportActionBar();
        supportActionBar.setDisplayHomeAsUpEnabled(true);
        // FIXME: do this with a theme attr instead.
        ColorDrawable color = new ColorDrawable(getResources().getColor(
                R.color.shaded_background));
        supportActionBar.setBackgroundDrawable(color);

        ParticleDevice device = getIntent().getParcelableExtra(EXTRA_DEVICE);
        String name = truthy(device.getName()) ? device.getName() : "(Unnamed device)";
        setTitle(name);
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // we handle both the context device row actions here and our own
        getMenuInflater().inflate(R.menu.context_device_row, menu);
        return true;
    }

//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        int actionId = item.getItemId();
//        if (DeviceActionsHelper.takeActionForDevice(actionId, getActivity(), device)) {
//            return true;
//
//        } else if (actionId == R.id.action_device_clear_tinker) {
//            prefs.clearTinker(device.getID());
//            for (Pin pin : allPins) {
//                pin.setConfiguredAction(PinAction.NONE);
//                pin.reset();
//            }
//            return true;
//
//        } else if (DeviceMenuUrlHandler.handleActionItem(getActivity(), actionId, item.getTitle())) {
//            return true;
//
//        } else {
//            return super.onOptionsItemSelected(item);
//        }
//    }
}
