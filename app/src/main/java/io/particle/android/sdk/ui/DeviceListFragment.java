package io.particle.android.sdk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;
import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.ArrayList;
import java.util.List;

import io.particle.android.sdk.DevicesLoader;
import io.particle.android.sdk.cloud.SparkDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.tinker.TinkerFragment;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.truthy;

/**
 * A list fragment representing a list of Devices. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link TinkerFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class DeviceListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<List<SparkDevice>> {

    public interface Callbacks {
        void onDeviceSelected(String id);
    }

    private static final String STATE_ACTIVATED_POSITION = "activated_position";


    // A no-op impl of {@link Callbacks}. Used when this fragment is not attached to an activity.
    private static final Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public void onDeviceSelected(String id) {
        }
    };

    private static final TLog log = TLog.get(DeviceListFragment.class);

    private SwipeRefreshLayout refreshLayout;
    private FloatingActionsMenu fabMenu;
    private Callbacks mCallbacks = sDummyCallbacks;
    private int mActivatedPosition = ListView.INVALID_POSITION;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = EZ.getCallbacksOrThrow(this, Callbacks.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        getLoaderManager().initLoader(R.id.device_list_devices_loader_id, null, this);
        // FIXME: replace with spinner
        Toaster.s(this, "Loading devices...");
        View top = inflater.inflate(R.layout.fragment_device_list, container, false);

        // add a blank footer to make sure the FAB never obscures an item at the bottom of the list
        ListView lv = Ui.findView(top, android.R.id.list);
        View blankFooter = inflater.inflate(android.R.layout.simple_list_item_1, null);
        blankFooter.setBackgroundColor(getResources().getColor(android.R.color.transparent));
        lv.addFooterView(blankFooter);

        setListAdapter(new DeviceListAdapter(getActivity(), android.R.id.text1));

        return top;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

        fabMenu = Ui.findView(view, R.id.add_device_fab);
        AddFloatingActionButton addPhoton = Ui.findView(view, R.id.action_set_up_a_photon);
        AddFloatingActionButton addCore = Ui.findView(view, R.id.action_set_up_a_core);

        addPhoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParticleDeviceSetupLibrary.startDeviceSetup(getActivity());
                new ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver() {

                    @Override
                    public void onSetupSuccess(long l) {
                        log.d("Successfully set up " + l);
                    }

                    @Override
                    public void onSetupFailure() {
                        log.w("Device not set up.");
                    }
                }.register(getActivity().getApplicationContext());
                fabMenu.collapse();
            }
        });
        addCore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String coreAppPkg = "io.spark.core.android";
                Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(coreAppPkg);
                if (intent == null) {
                    intent = new Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse("market://details?id=" + coreAppPkg));
                }
                startActivity(intent);
                fabMenu.collapse();
            }
        });

        refreshLayout = Ui.findView(view, R.id.refresh_layout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshDevices();
            }
        });
    }

    public void showMenu(View v, final SparkDevice device) {
        PopupMenu popup = new PopupMenu(v.getContext(), v);
        popup.inflate(R.menu.context_device_row);
        popup.setOnMenuItemClickListener(DeviceActionsHelper.buildPopupMenuHelper(this, device));
        popup.show();
    }

    private void refreshDevices() {
        Loader<Object> loader = getLoaderManager().getLoader(R.id.device_list_devices_loader_id);
        loader.forceLoad();
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshDevices();
    }

    @Override
    public void onStop() {
        refreshLayout.setRefreshing(false);
        super.onStop();
    }


    @Override
    public void onDetach() {
        super.onDetach();
        // Reset the active callbacks interface to the dummy implementation.
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        super.onListItemClick(listView, view, position, id);

        if (position >= getListAdapter().getCount()) {
            // we're at the footer view, do nothing.
            return;
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        final SparkDevice device = (SparkDevice) getListAdapter().getItem(position);

        if (device.isFlashing()) {
            Toaster.s(getActivity(), "Device is being flashed, please wait for the flashing process to end first");

        } else if (!device.isConnected()) {
            new MaterialDialog.Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title("Device offline")
                    .content("This device is offline, please turn it on and refresh in order to Tinker with it.")
                    .positiveText("OK")
                    .show();

        } else if (!device.isRunningTinker()) {
            new MaterialDialog.Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title("Device not running Tinker")
                    .content("This device is not running Tinker firmware.")
                    .positiveText("Re-flash Tinker")
                    .neutralText("Tinker anyway")
                    .negativeText("Cancel")
                    .callback(new MaterialDialog.ButtonCallback() {
                        @Override
                        public void onPositive(MaterialDialog dialog) {
                            super.onPositive(dialog);
                            DeviceActionsHelper.takeActionForDevice(R.id.action_device_flash_tinker, getActivity(), device);
                        }

                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            super.onNeutral(dialog);
                            mCallbacks.onDeviceSelected(device.getID());
                        }
                    })
                    .show();


        } else {
            mCallbacks.onDeviceSelected(device.getID());
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    public boolean onBackPressed() {
        if (fabMenu.isExpanded()) {
            fabMenu.collapse();
            return true;
        } else {
            return false;
        }
    }

    // When activate-on-click-mode is on, list items will be given the 'activated' state when touched.
    public void setActivateOnItemClick(boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.
        getListView().setChoiceMode(activateOnItemClick
                ? ListView.CHOICE_MODE_SINGLE
                : ListView.CHOICE_MODE_NONE);
    }

    private void setActivatedPosition(int position) {
        if (position == ListView.INVALID_POSITION) {
            getListView().setItemChecked(mActivatedPosition, false);
        } else {
            getListView().setItemChecked(position, true);
        }

        mActivatedPosition = position;
    }

    @Override
    public Loader<List<SparkDevice>> onCreateLoader(int i, Bundle bundle) {
        return new DevicesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<SparkDevice>> loader, List<SparkDevice> sparkDevices) {
        refreshLayout.setRefreshing(false);
        @SuppressWarnings("unchecked")
        DeviceListAdapter listAdapter = (DeviceListAdapter) getListAdapter();
        listAdapter.clear();
        listAdapter.addAll(sparkDevices);
    }

    @Override
    public void onLoaderReset(Loader<List<SparkDevice>> loader) {
        // no-op
    }


    class DeviceListAdapter extends ArrayAdapter<SparkDevice> {

        public DeviceListAdapter(Context context, int resource) {
            super(context, resource, new ArrayList<SparkDevice>());
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_device_list, parent, false);
                if (position % 2 == 0) {
                    convertView.setBackgroundResource(R.color.shaded_background);
                }
            }
            final SparkDevice device = getItem(position);

            TextView modelName = Ui.findView(convertView, R.id.product_model_name);
            ImageView productImage = Ui.findView(convertView, R.id.product_image);
            switch (device.getDeviceType()) {
                case CORE:
                    modelName.setText("Core");
                    productImage.setImageResource(R.drawable.core_vector);
                    break;

                default :
                    modelName.setText("Photon");
                    productImage.setImageResource(R.drawable.photon_vector_small);
                    break;

            }

            Pair<String, Integer> statusTextAndColoredDot = getStatusTextAndColoredDot(device);
            TextView statusText = Ui.findView(convertView, R.id.online_status);
            statusText.setCompoundDrawablesWithIntrinsicBounds(0, 0, statusTextAndColoredDot.second, 0);
            statusText.setText(statusTextAndColoredDot.first);

            Ui.setText(convertView, R.id.product_id, device.getID().toUpperCase());
            String name = truthy(device.getName())
                    ? device.getName() : getString(R.string.unnamed_device);
            Ui.setText(convertView, R.id.product_name, name);

            Ui.findView(convertView, R.id.context_menu).setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showMenu(view, device);
                        }
                    }
            );

            return convertView;
        }

        private Pair<String, Integer> getStatusTextAndColoredDot(SparkDevice device) {
            int dot;
            String msg;
            if (device.isFlashing()) {
                dot = R.drawable.device_flashing_dot;
                msg = "Flashing";

            } else if (device.isConnected()) {
                if (device.isRunningTinker()) {
                    dot = R.drawable.online_dot;
                    msg = "Online";

                } else {
                    dot = R.drawable.online_non_tinker_dot;
                    msg = "Online, non-Tinker";
                }

            } else {
                dot = R.drawable.offline_dot;
                msg = "Offline";
            }
            return Pair.create(msg, dot);
        }
    }

}
