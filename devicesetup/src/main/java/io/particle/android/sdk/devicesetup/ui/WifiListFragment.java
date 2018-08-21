package io.particle.android.sdk.devicesetup.ui;


import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.particle.android.sdk.devicesetup.R;
import io.particle.android.sdk.devicesetup.model.WifiNetwork;
import io.particle.android.sdk.utils.EZ;
import io.particle.android.sdk.utils.TLog;
import io.particle.android.sdk.utils.ui.Ui;

import static io.particle.android.sdk.utils.Py.set;


public class WifiListFragment<T extends WifiNetwork> extends ListFragment
        implements LoaderManager.LoaderCallbacks<Set<T>> {


    public interface Client<T extends WifiNetwork> {

        void onNetworkSelected(T selectedNetwork);

        Loader<Set<T>> createLoader(int id, Bundle args);

        void onLoadFinished();

        String getListEmptyText();

        int getAggroLoadingTimeMillis();

    }


    private static final TLog log = TLog.get(WifiListFragment.class);


    private WifiNetworkAdapter adapter;
    private Runnable aggroLoadingRunnable;
    private Handler aggroLoadingHandler;
    private Client<T> client;

    private Set<T> previousData = set();

    public void scanAsync() {
        if (isDetached() || client == null) {
            stopAggroLoading();
        } else {
            // FIXME: just use a rsrc ID for the loader ID instead of this madness.
            Loader<Object> loader = getLoaderManager().getLoader(getClass().hashCode());
            loader.forceLoad();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        client = EZ.getCallbacksOrThrow(this, Client.class);
        if (aggroLoadingHandler == null) {
            aggroLoadingHandler = new Handler();
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new WifiNetworkAdapter(getActivity());
        setEmptyText(client.getListEmptyText());
        getLoaderManager().initLoader(getClass().hashCode(), null, this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setVerticalScrollBarEnabled(true);
        getListView().setScrollbarFadingEnabled(false);
    }

    @Override
    public void onStart() {
        super.onStart();
        scanAsync();
        startAggroLoading();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopAggroLoading();
    }

    @Override
    public void onDetach() {
        client = null;
        super.onDetach();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        T selectedNetwork = adapter.getItem(position);
        if (null != client) {
            client.onNetworkSelected(selectedNetwork);
        } else {
            log.e("Client was null");
        }
    }

    @Override
    public Loader<Set<T>> onCreateLoader(int id, Bundle args) {
        return client.createLoader(id, args);
    }

    @Override
    public void onLoadFinished(Loader<Set<T>> loader, Set<T> data) {
        log.d("new scan results: " + data);

        data = (data == null) ? Collections.emptySet() : data;

        // only do this work if our data has actually changed
        if (!previousData.equals(data)) {
            previousData = data;
            adapter.clear();

            List<T> asList = new ArrayList<>(data);
            Collections.sort(asList, (lhs, rhs) -> lhs.getSsid().compareTo(rhs.getSsid()));

            adapter.addAll(asList);
        }

        // setting the adapter at this point, instead of in onCreateView(), etc, means we get
        // a loading spinner for free -- see ListFragment source
        if (getListAdapter() == null) {  // no list shown yet
            setListAdapter(adapter);
        }

        if (client != null) {
            client.onLoadFinished();
        }
    }

    @Override
    public void onLoaderReset(Loader<Set<T>> loader) {
        adapter.clear();
    }


    public void startAggroLoading() {
        if (aggroLoadingRunnable == null) {
            scheduleNextAggroLoad();
        }
    }

    private void scheduleNextAggroLoad() {
        if (client == null) {
            stopAggroLoading();
            return;
        }

        aggroLoadingRunnable = () -> {
            log.d("Running aggro loading");
            scanAsync();
            aggroLoadingRunnable = null;
            scheduleNextAggroLoad();
        };
        aggroLoadingHandler.postDelayed(aggroLoadingRunnable, client.getAggroLoadingTimeMillis());
    }

    public void stopAggroLoading() {
        if (aggroLoadingRunnable != null) {
            aggroLoadingHandler.removeCallbacks(aggroLoadingRunnable);
            aggroLoadingRunnable = null;
        }
    }


    private class WifiNetworkAdapter extends ArrayAdapter<T> {

        WifiNetworkAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_1);
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.row_wifi_scan_result, parent, false);

                ImageView theWifi = Ui.findView(convertView, R.id.the_wifi);
                Drawable whiteWifi = Ui.getTintedDrawable(getContext(), R.drawable.the_wifi,
                        R.color.element_tint_color);
                theWifi.setImageDrawable(whiteWifi);

                ImageView securityIcon = Ui.findView(convertView, R.id.wifi_security_indicator_icon);
                Drawable whiteLock = Ui.getTintedDrawable(getContext(), R.drawable.lock,
                        R.color.element_tint_color);
                securityIcon.setImageDrawable(whiteLock);
            }

            T wifiNetwork = getItem(position);
            if (wifiNetwork != null) {
                Ui.setText(convertView, android.R.id.text1, wifiNetwork.getSsid().toString());
                Ui.findView(convertView, R.id.wifi_security_indicator_icon)
                        .setVisibility(wifiNetwork.isSecured() ? View.VISIBLE : View.GONE);
            }
            return convertView;
        }

    }

}
