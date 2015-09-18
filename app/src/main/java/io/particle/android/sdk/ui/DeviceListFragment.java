package io.particle.android.sdk.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.Builder;
import com.afollestad.materialdialogs.Theme;
import com.getbase.floatingactionbutton.AddFloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.tumblr.bookends.Bookends;

import java.util.List;

import io.particle.android.sdk.DevicesLoader;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver;
import io.particle.android.sdk.ui.ItemClickSupport.OnItemClickListener;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;


public class DeviceListFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<List<ParticleDevice>>{


    public interface Callbacks {
        void onDeviceSelected(ParticleDevice device);
    }


    private static final TLog log = TLog.get(DeviceListFragment.class);

    // A no-op impl of {@link Callbacks}. Used when this fragment is not attached to an activity.
    private static final Callbacks dummyCallbacks = new Callbacks() {
        @Override
        public void onDeviceSelected(ParticleDevice device) {
            // no-op
        }
    };

    private SwipeRefreshLayout refreshLayout;
    private FloatingActionsMenu fabMenu;
    private DeviceListAdapter adapter;
    private Bookends<DeviceListAdapter> bookends;

    private Callbacks callbacks = dummyCallbacks;
    private DeviceSetupCompleteReceiver deviceSetupCompleteReceiver;


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        callbacks = EZ.getCallbacksOrThrow(this, Callbacks.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_device_list2, container, false);

        RecyclerView rv = Ui.findView(top, R.id.device_list);
        rv.setHasFixedSize(true);  // perf. optimization
        LinearLayoutManager layoutManager = new LinearLayoutManager(inflater.getContext());
        rv.setLayoutManager(layoutManager);

        @SuppressLint("InflateParams")
        View myHeader = inflater.inflate(R.layout.device_list_header, null);
        myHeader.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        @SuppressLint("InflateParams")
        View myFooter = inflater.inflate(R.layout.device_list_header, null);
        Ui.setText(myFooter, R.id.header_text, "");

        adapter = new DeviceListAdapter(getActivity());
        // Add them as headers / footers
        bookends = new Bookends<>(adapter);
        bookends.addHeader(myHeader);
        bookends.addFooter(myFooter);

        rv.setAdapter(bookends);

        ItemClickSupport.addTo(rv).setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                // subtracting 1 from position because of header.  This is gross, but it's simple
                // and in this case adequate, so #SHIPIT.
                onDeviceRowClicked(recyclerView, position - 1, v);
            }
        });

        return top;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        fabMenu = Ui.findView(view, R.id.add_device_fab);
        AddFloatingActionButton addPhoton = Ui.findView(view, R.id.action_set_up_a_photon);
        AddFloatingActionButton addCore = Ui.findView(view, R.id.action_set_up_a_core);

        addPhoton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addPhotonDevice();
            }
        });
        addCore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addSparkCoreDevice();
            }
        });

        refreshLayout = Ui.findView(view, R.id.refresh_layout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshDevices();
            }
        });

        deviceSetupCompleteReceiver = new ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver() {
            @Override
            public void onSetupSuccess(long l) {
                log.d("Successfully set up " + l);
                refreshDevices();
            }

            @Override
            public void onSetupFailure() {
                log.w("Device not set up.");
            }
        };
        deviceSetupCompleteReceiver.register(getActivity());

        getLoaderManager().initLoader(R.id.device_list_devices_loader_id, null, this);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshDevices();
    }

    @Override
    public void onStop() {
        super.onStop();
        refreshLayout.setRefreshing(false);
        fabMenu.collapse();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        callbacks = dummyCallbacks;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deviceSetupCompleteReceiver.unregister(getActivity());
    }

    @Override
    public Loader<List<ParticleDevice>> onCreateLoader(int i, Bundle bundle) {
        return new DevicesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<ParticleDevice>> loader, List<ParticleDevice> sparkDevices) {
        refreshLayout.setRefreshing(false);
        adapter.clear();
        adapter.addAll(sparkDevices);
        bookends.notifyDataSetChanged();
    }

    @Override
    public void onLoaderReset(Loader<List<ParticleDevice>> loader) {
        // no-op
    }

    private void onDeviceRowClicked(RecyclerView recyclerView, int position, View view) {
        log.i("Clicked on item at position: #" + position);
        if (position >= bookends.getItemCount() || position == -1) {
            // we're at the header or footer view, do nothing.
            return;
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        final ParticleDevice device = adapter.getItem(position);

        if (device.isFlashing()) {
            Toaster.s(getActivity(),
                    "Device is being flashed, please wait for the flashing process to end first");

        } else if (!device.isConnected()) {
            new Builder(getActivity())
                    .theme(Theme.LIGHT)
                    .title("Device offline")
                    .content(R.string.err_msg_device_is_offline)
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
                            DeviceActionsHelper.takeActionForDevice(
                                    R.id.action_device_flash_tinker, getActivity(), device);
                        }

                        @Override
                        public void onNeutral(MaterialDialog dialog) {
                            super.onNeutral(dialog);
                            callbacks.onDeviceSelected(device);
                        }
                    })
                    .show();


        } else {
            callbacks.onDeviceSelected(device);
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

    private void addPhotonDevice() {
        ParticleDeviceSetupLibrary.startDeviceSetup(getActivity());
        fabMenu.collapse();
    }

    private void addSparkCoreDevice() {
        String coreAppPkg = "io.spark.core.android";
        // Is the spark core app already installed?
        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage(coreAppPkg);
        if (intent == null) {
            // Nope.  Send the user to the store.
            intent = new Intent(Intent.ACTION_VIEW)
                    .setData(Uri.parse("market://details?id=" + coreAppPkg));
        }
        startActivity(intent);
        fabMenu.collapse();
    }

    private void refreshDevices() {
        Loader<Object> loader = getLoaderManager().getLoader(R.id.device_list_devices_loader_id);
        loader.forceLoad();
    }


    static class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

        static class ViewHolder extends RecyclerView.ViewHolder {

            final View topLevel;
            final TextView modelName;
            final ImageView productImage;
            final TextView deviceName;
            final TextView statusTextWithIcon;
            final TextView productId;
            final ImageView overflowMenuIcon;

            public ViewHolder(View itemView) {
                super(itemView);
                topLevel = itemView;
                modelName = Ui.findView(itemView, R.id.product_model_name);
                productImage = Ui.findView(itemView, R.id.product_image);
                deviceName = Ui.findView(itemView, R.id.product_name);
                statusTextWithIcon = Ui.findView(itemView, R.id.online_status);
                productId = Ui.findView(itemView, R.id.product_id);
                overflowMenuIcon = Ui.findView(itemView, R.id.context_menu);
            }
        }


        private final List<ParticleDevice> devices = list();
        private final FragmentActivity activity;

        DeviceListAdapter(FragmentActivity activity) {
            this.activity = activity;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.row_device_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final ParticleDevice device = devices.get(position);

            if (position % 2 == 0) {
                holder.topLevel.setBackgroundResource(R.color.shaded_background);
            }

            switch (device.getDeviceType()) {
                case CORE:
                    holder.modelName.setText("Core");
                    holder.productImage.setImageResource(R.drawable.core_vector);
                    break;

                default :
                    holder.modelName.setText("Photon");
                    holder.productImage.setImageResource(R.drawable.photon_vector_small);
                    break;
            }

            Pair<String, Integer> statusTextAndColoredDot = getStatusTextAndColoredDot(device);
            holder.statusTextWithIcon.setText(statusTextAndColoredDot.first);
            holder.statusTextWithIcon.setCompoundDrawablesWithIntrinsicBounds(
                    0, 0, statusTextAndColoredDot.second, 0);

            holder.productId.setText(device.getID().toUpperCase());

            Context ctx = holder.topLevel.getContext();
            String name = truthy(device.getName())
                    ? device.getName()
                    : ctx.getString(R.string.unnamed_device);
            holder.deviceName.setText(name);

            holder.overflowMenuIcon.setOnClickListener(
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            showMenu(view, device);
                        }
                    }
            );
        }

        @Override
        public int getItemCount() {
            return devices.size();
        }

        void clear() {
            devices.clear();
            notifyDataSetChanged();
        }

        void addAll(List<ParticleDevice> toAdd) {
            devices.addAll(toAdd);
            notifyDataSetChanged();
        }

        ParticleDevice getItem(int position) {
            return devices.get(position);
        }

        private void showMenu(View v, final ParticleDevice device) {
            PopupMenu popup = new PopupMenu(v.getContext(), v);
            popup.inflate(R.menu.context_device_row);
            popup.setOnMenuItemClickListener(DeviceActionsHelper.buildPopupMenuHelper(activity, device));
            popup.show();
        }

        private Pair<String, Integer> getStatusTextAndColoredDot(ParticleDevice device) {
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
