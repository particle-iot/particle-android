package io.particle.android.sdk.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import io.particle.android.sdk.accountsetup.LoginActivity;
import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.tinker.TinkerFragment;
import io.particle.android.sdk.utils.SoftAPConfigRemover;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

/**
 * An activity representing a list of Devices. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link TinkerActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 * <p/>
 * The activity makes heavy use of fragments. The list of items is a
 * {@link DeviceListFragment} and the item details
 * (if present) is a {@link TinkerFragment}.
 * <p/>
 * This activity also implements the required
 * {@link DeviceListFragment.Callbacks} interface
 * to listen for item selections.
 */
public class DeviceListActivity extends BaseActivity implements DeviceListFragment.Callbacks {

    // Whether or not the activity is in two-pane mode, i.e. running on a tablet
    private boolean mTwoPane;
    private SoftAPConfigRemover softAPConfigRemover;
    private DeviceListFragment deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);

        softAPConfigRemover = new SoftAPConfigRemover(this);

        if (Ui.findView(this, R.id.device_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            // FIXME: need to impl in RecyclerView if we want two-pane mode
//            ((DeviceListFragment) getSupportFragmentManager()
//                    .findFragmentById(R.id.device_list))
//                    .setActivateOnItemClick(true);
        }

        deviceList = Ui.findFrag(this, R.id.fragment_device_list);
        // TODO: If exposing deep links into your app, handle intents here.

        // Show the Up button in the action bar.
        ActionBar supportActionBar = getSupportActionBar();
        if (supportActionBar != null) {
            // FIXME: do this with a theme attr instead.
            Drawable background = ContextCompat.getDrawable(this, R.drawable.ic_triangy_toolbar_background);
            supportActionBar.setBackgroundDrawable(background);
        }
        setTitle(R.string.your_devices);
    }

    @Override
    protected void onStart() {
        super.onStart();
        softAPConfigRemover.removeAllSoftApConfigs();
        softAPConfigRemover.reenableWifiNetworks();
    }

    @Override
    public void onBackPressed() {
        if (deviceList == null || !deviceList.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_log_out) {
            final ParticleCloud cloud = ParticleCloudSDK.getCloud();
            cloud.logOut();
            startActivity(new Intent(DeviceListActivity.this, LoginActivity.class));
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //region DeviceListFragment.Callbacks
    @Override
    public void onDeviceSelected(@NonNull ParticleDevice device) {
        // FIXME: re-enable
//
//        return;
//
//        if (mTwoPane) {
//            // In two-pane mode, show the detail view in this activity by
//            // adding or replacing the detail fragment using a
//            // fragment transaction.
//            getSupportFragmentManager()
//                    .beginTransaction()
//                    .replace(R.id.device_detail_container, TinkerFragment.newInstance(id))
//                    .commit();
//
//        } else {
        // In single-pane mode, simply start the detail activity
        // for the selected item.
        startActivity(TinkerActivity.buildIntent(this, device));
//        }
    }
    //endregion
}
