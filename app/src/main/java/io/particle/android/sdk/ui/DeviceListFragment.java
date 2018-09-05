package io.particle.android.sdk.ui;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.Snackbar.Callback;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.particle.android.sdk.DevicesLoader;
import io.particle.android.sdk.DevicesLoader.DevicesLoadResult;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary;
import io.particle.android.sdk.devicesetup.ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver;
import io.particle.android.sdk.ui.Comparators.BooleanComparator;
import io.particle.android.sdk.ui.Comparators.ComparatorChain;
import io.particle.android.sdk.ui.Comparators.NullComparator;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Toaster;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.mesh.setup.ui.MeshSetupActivity;
import io.particle.sdk.app.R;

import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.BLUZ;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.CORE;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.DIGISTUMP_OAK;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.ELECTRON;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.P1;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.PHOTON;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RASPBERRY_PI;
import static io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType.RED_BEAR_DUO;
import static io.particle.android.sdk.utils.Py.list;
import static io.particle.android.sdk.utils.Py.truthy;
import static java.util.Objects.requireNonNull;

//FIXME enabling & disabling system events on each refresh as it collides with fetching devices in parallel
//@ParametersAreNonnullByDefault
public class DeviceListFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<DevicesLoadResult> {

    interface Callbacks {
        void onDeviceSelected(ParticleDevice device);
    }

    private static final TLog log = TLog.get(DeviceListFragment.class);

    // A no-op impl of {@link Callbacks}. Used when this fragment is not attached to an activity.
    private static final Callbacks dummyCallbacks = device -> { /* no-op */ };

    @BindView(R.id.add_device_fab) FloatingActionsMenu fabMenu;
    @BindView(R.id.refresh_layout) SwipeRefreshLayout refreshLayout;
    @BindView(R.id.empty_message) TextView emptyMessage;
    private DeviceListAdapter adapter;
    // FIXME: naming, document better
    private ProgressBar partialContentBar;
    private boolean isLoadingSnackbarVisible;

    private final Queue<Long> subscribeIds = new ConcurrentLinkedQueue<>();
    private final ReloadStateDelegate reloadStateDelegate = new ReloadStateDelegate();
    private final Comparator<ParticleDevice> comparator = helpfulOrderDeviceComparator();

    private Callbacks callbacks = dummyCallbacks;
    private DeviceSetupCompleteReceiver deviceSetupCompleteReceiver;

    @OnClick(R.id.action_set_up_a_xenon)
    public void addXenon() {
        addXenonDevice();
        fabMenu.collapse();
    }

    @OnClick(R.id.action_set_up_a_photon)
    public void addPhoton() {
        addPhotonDevice();
        fabMenu.collapse();
    }

    @OnClick(R.id.action_set_up_a_core)
    public void addCore() {
        addSparkCoreDevice();
        fabMenu.collapse();
    }

    @OnClick(R.id.action_set_up_an_electron)
    public void addElectron() {
        addElectronDevice();
        fabMenu.collapse();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        callbacks = EZ.getCallbacksOrThrow(this, Callbacks.class);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_device_list2, container, false);

        RecyclerView rv = Ui.findView(top, R.id.device_list);
        rv.setHasFixedSize(true);  // perf. optimization
        LinearLayoutManager layoutManager = new LinearLayoutManager(inflater.getContext());
        rv.setLayoutManager(layoutManager);
        rv.addItemDecoration(new DividerItemDecoration(requireNonNull(getContext()), LinearLayout.VERTICAL));

        partialContentBar = (ProgressBar) inflater.inflate(R.layout.device_list_footer, (ViewGroup) top, false);
        partialContentBar.setVisibility(View.INVISIBLE);
        partialContentBar.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        adapter = new DeviceListAdapter(requireNonNull(getActivity()));
        rv.setAdapter(adapter);
        ItemClickSupport.addTo(rv).setOnItemClickListener((recyclerView, position, v) -> onDeviceRowClicked(position));
        return top;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        refreshLayout.setOnRefreshListener(this::refreshDevices);

        deviceSetupCompleteReceiver = new ParticleDeviceSetupLibrary.DeviceSetupCompleteReceiver() {
            @Override
            public void onSetupSuccess(String id) {
                log.d("Successfully set up " + id);
            }

            @Override
            public void onSetupFailure() {
                log.w("Device not set up.");
            }
        };
        deviceSetupCompleteReceiver.register(getActivity());

        getLoaderManager().initLoader(R.id.device_list_devices_loader_id, null, this);
        refreshLayout.setRefreshing(true);

        if (savedInstanceState != null) {
            adapter.filter(savedInstanceState.getString("txtFilterState"));
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("txtFilterState", adapter.getTextFilter());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) {
            List<ParticleDevice> devices = adapter.getItems();
            subscribeToSystemEvents(devices, false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshDevices();
    }

    @Override
    public void onPause() {
        if (adapter != null) {
            List<ParticleDevice> devices = adapter.getItems();
            subscribeToSystemEvents(devices, true);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        refreshLayout.setRefreshing(false);
        fabMenu.collapse();
        reloadStateDelegate.reset();
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

    @NonNull
    @Override
    public Loader<DevicesLoadResult> onCreateLoader(int i, @Nullable Bundle bundle) {
        return new DevicesLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<DevicesLoadResult> loader, DevicesLoadResult result) {
        refreshLayout.setRefreshing(false);

        ArrayList<ParticleDevice> devices = new ArrayList<>(result.devices);
        Collections.sort(devices, comparator);

        reloadStateDelegate.onDeviceLoadFinished(loader, result);

        adapter.clear();
        adapter.addAll(devices);
        adapter.notifyDataSetChanged();

        emptyMessage.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
        //subscribe to system updates
        subscribeToSystemEvents(devices, false);
    }

    private void subscribeToSystemEvents(List<ParticleDevice> devices, boolean revertSubscription) {
        for (ParticleDevice device : devices) {
            new AsyncTask<ParticleDevice, Void, Void>() {
                @Override
                protected Void doInBackground(ParticleDevice... particleDevices) {
                    try {
                        if (revertSubscription) {
                            for (Long id : subscribeIds) {
                                device.unsubscribeFromEvents(id);
                            }
                        } else {
                            subscribeIds.add(device.subscribeToEvents("spark/status", new ParticleEventHandler() {
                                @Override
                                public void onEventError(Exception e) {
                                    //ignore for now, events aren't vital
                                }

                                @Override
                                public void onEvent(String eventName, ParticleEvent particleEvent) {
                                    refreshDevices();
                                }
                            }));
                        }
                    } catch (IOException | ParticleCloudException ignore) {
                        //ignore for now, events aren't vital
                    }
                    return null;
                }
            }.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, device);
        }
    }

    @Override
    public void onLoaderReset(Loader<DevicesLoadResult> loader) {
        // no-op
    }

    private void onDeviceRowClicked(int position) {
        log.i("Clicked on item at position: #" + position);
        if (position >= adapter.getItemCount() || position == -1) {
            // we're at the header or footer view, do nothing.
            return;
        }

        // Notify the active callbacks interface (the activity, if the
        // fragment is attached to one) that an item has been selected.
        final ParticleDevice device = adapter.getItem(position);

        if (device.isFlashing()) {
            Toaster.s(getActivity(),
                    "Device is being flashed, please wait for the flashing process to end first");
        } else if (!device.isConnected() || !device.isRunningTinker()) {
            Activity activity = getActivity();

            if (activity != null) {
                activity.startActivity(InspectorActivity.buildIntent(activity, device));
            }
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

    private void addXenonDevice() {
        startActivity(new Intent(getActivity(), MeshSetupActivity.class));
    }

    private void addPhotonDevice() {
        ParticleDeviceSetupLibrary.startDeviceSetup(requireNonNull(getActivity()), DeviceListActivity.class);
    }

    private void addSparkCoreDevice() {
        try {
            String coreAppPkg = "io.spark.core.android";
            // Is the spark core app already installed?
            Intent intent = requireNonNull(getActivity()).getPackageManager().getLaunchIntentForPackage(coreAppPkg);
            if (intent == null) {
                // Nope.  Send the user to the store.
                intent = new Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse("market://details?id=" + coreAppPkg));
            }
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            Toast.makeText(getActivity(), "Cannot find spark core application.", Toast.LENGTH_SHORT).show();
        }
    }

    private void addElectronDevice() {
        //        Intent intent = (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP)
//                ? new Intent(getActivity(), ElectronSetupActivity.class)
//                : new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.electron_setup_uri)));
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.electron_setup_uri)));
        startActivity(intent);
    }

    private void refreshDevices() {
        if (adapter != null) {
            List<ParticleDevice> devices = adapter.getItems();
            subscribeToSystemEvents(devices, true);
        }
        Loader<Object> loader = getLoaderManager().getLoader(R.id.device_list_devices_loader_id);
        loader.forceLoad();
    }

    public void filter(ArrayList<ParticleDevice.ParticleDeviceType> typeArrayList) {
        adapter.filter(typeArrayList);
        emptyMessage.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public void filter(String query) {
        adapter.filter(query);
        emptyMessage.setVisibility(adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }

    public String getTextFilter() {
        return adapter != null ? adapter.getTextFilter() : null;
    }

    static class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {

        static class ViewHolder extends RecyclerView.ViewHolder {

            final View topLevel;
            @BindView(R.id.product_model_name) TextView modelName;
            @BindView(R.id.product_image) AppCompatImageView productImage;
            @BindView(R.id.online_status_image) AppCompatImageView statusIcon;
            @BindView(R.id.product_name) TextView deviceName;
            @BindView(R.id.online_status) TextView statusTextWithIcon;

            ViewHolder(View itemView) {
                super(itemView);
                topLevel = itemView;
                ButterKnife.bind(this, itemView);
            }
        }

        private final List<ParticleDevice> devices = list();
        private final List<ParticleDevice> filteredData = list();
        private final FragmentActivity activity;
        private Drawable defaultBackground;
        private String textFilter = "";
        private List<ParticleDevice.ParticleDeviceType> typeFilters = list(PHOTON, CORE, ELECTRON,
                RASPBERRY_PI, P1, RED_BEAR_DUO, DIGISTUMP_OAK, BLUZ);

        DeviceListAdapter(FragmentActivity activity) {
            this.activity = activity;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.row_device_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            final ParticleDevice device = filteredData.get(position);

            if (defaultBackground == null) {
                defaultBackground = holder.topLevel.getBackground();
            }
            holder.topLevel.setBackgroundResource(R.color.device_item_bg);

            switch (device.getDeviceType()) {
                case CORE:
                    holder.modelName.setText(R.string.core);
                    holder.productImage.setImageResource(R.drawable.core_vector);
                    break;

                case PHOTON:
                    holder.modelName.setText(R.string.photon);
                    holder.productImage.setImageResource(R.drawable.photon_vector_small);
                    break;

                case ELECTRON:
                    holder.modelName.setText(R.string.electron);
                    holder.productImage.setImageResource(R.drawable.electron_vector_small);
                    break;

                case RASPBERRY_PI:
                    holder.modelName.setText(R.string.raspberry);
                    holder.productImage.setImageResource(R.drawable.pi_vector);
                    break;

                case P1:
                    holder.modelName.setText(R.string.p1);
                    holder.productImage.setImageResource(R.drawable.p1_vector);
                    break;

                case RED_BEAR_DUO:
                    holder.modelName.setText(R.string.red_bear_duo);
                    holder.productImage.setImageResource(R.drawable.red_bear_duo_vector);
                    break;

                default:
                    holder.modelName.setText(R.string.unknown);
                    holder.productImage.setImageResource(R.drawable.unknown_vector);
                    break;
            }

            Pair<String, Integer> statusTextAndColoredDot = getStatusTextAndColoredDot(device);
            holder.statusTextWithIcon.setText(statusTextAndColoredDot.first);
            holder.statusIcon.setImageResource(statusTextAndColoredDot.second);

            if (device.isConnected()) {
                Animation animFade = AnimationUtils.loadAnimation(activity, R.anim.fade_in_out);
                animFade.setStartOffset(position * 1000);
                holder.statusIcon.startAnimation(animFade);
            }

            Context ctx = holder.topLevel.getContext();
            String name = truthy(device.getName())
                    ? device.getName()
                    : ctx.getString(R.string.unnamed_device);
            holder.deviceName.setText(name);
        }

        @Override
        public int getItemCount() {
            return filteredData.size();
        }

        void clear() {
            devices.clear();
            filteredData.clear();
            notifyDataSetChanged();
        }

        void addAll(List<ParticleDevice> toAdd) {
            devices.addAll(toAdd);
            filter(textFilter, typeFilters);
        }

        void filter(@Nullable String query) {
            textFilter = query;
            filteredData.clear();
            notifyDataSetChanged();

            filter(query, typeFilters);
        }

        void filter(List<ParticleDevice.ParticleDeviceType> typeArrayList) {
            typeFilters = typeArrayList;
            filteredData.clear();
            notifyDataSetChanged();

            filter(textFilter, typeArrayList);
        }

        void filter(@Nullable String query, List<ParticleDevice.ParticleDeviceType> typeArrayList) {
            for (ParticleDevice device : devices) {
                if ((containsFilter(device.getName(), query) || containsFilter(device.getDeviceType().name(), query)
                        || containsFilter(device.getCurrentBuild(), query) || containsFilter(device.getIccid(), query)
                        || containsFilter(device.getID(), query) || containsFilter(device.getImei(), query))
                        && typeArrayList.contains(device.getDeviceType())) {
                    filteredData.add(device);
                    notifyItemInserted(devices.indexOf(device));
                }
            }
        }

        ParticleDevice getItem(int position) {
            return devices.get(position);
        }

        List<ParticleDevice> getItems() {
            return devices;
        }

        String getTextFilter() {
            return textFilter;
        }

        private Pair<String, Integer> getStatusTextAndColoredDot(ParticleDevice device) {
            int dot;
            String msg;
            if (device.isFlashing()) {
                dot = R.drawable.device_flashing_dot;
                msg = "";

            } else if (device.isConnected()) {
                if (device.isRunningTinker()) {
                    dot = R.drawable.online_dot;
                    msg = "Tinker";

                } else {
                    dot = R.drawable.online_non_tinker_dot;
                    msg = "";
                }

            } else {
                dot = R.drawable.offline_dot;
                msg = "";
            }
            return Pair.create(msg, dot);
        }
    }

    private static boolean containsFilter(@Nullable String value, @Nullable String query) {
        return value != null && value.contains(query != null ? query : "");
    }

    private static Comparator<ParticleDevice> helpfulOrderDeviceComparator() {
        Comparator<ParticleDevice> deviceOnlineStatusComparator = (lhs, rhs) -> BooleanComparator.getTrueFirstComparator()
                .compare(lhs.isConnected(), rhs.isConnected());
        NullComparator<String> nullComparator = new NullComparator<>(false);
        Comparator<ParticleDevice> unnamedDevicesFirstComparator = (lhs, rhs) -> {
            String lhname = lhs.getName();
            String rhname = rhs.getName();
            return nullComparator.compare(lhname, rhname);
        };

        ComparatorChain<ParticleDevice> chain;
        chain = new ComparatorChain<>(deviceOnlineStatusComparator, false);
        chain.addComparator(unnamedDevicesFirstComparator, false);
        return chain;
    }


    private class ReloadStateDelegate {

        static final int MAX_RETRIES = 10;

        int retryCount = 0;

        void onDeviceLoadFinished(final Loader<DevicesLoadResult> loader, DevicesLoadResult result) {
            if (!result.isPartialResult) {
                reset();
                return;
            }

            retryCount++;
            if (retryCount > MAX_RETRIES) {
                // tried too many times, giving up. :(
                partialContentBar.setVisibility(View.INVISIBLE);
                return;
            }

            if (!isLoadingSnackbarVisible) {
                isLoadingSnackbarVisible = true;
                View view = getView();
                if (view != null) {
                    Snackbar.make(view, "Unable to load all devices", Snackbar.LENGTH_SHORT)
                            .addCallback(new Callback() {
                                @Override
                                public void onDismissed(Snackbar snackbar, int event) {
                                    super.onDismissed(snackbar, event);
                                    isLoadingSnackbarVisible = false;
                                }
                            }).show();
                }
            }

            partialContentBar.setVisibility(View.VISIBLE);
            ((DevicesLoader) loader).setUseLongTimeoutsOnNextLoad(true);
            // FIXME: is it OK to call forceLoad() in loader callbacks?  Test and be certain.
            EZ.runOnMainThread(() -> {
                if (isResumed()) {
                    loader.forceLoad();
                }
            });
        }

        void reset() {
            retryCount = 0;
            partialContentBar.setVisibility(View.INVISIBLE);
        }

    }

}
