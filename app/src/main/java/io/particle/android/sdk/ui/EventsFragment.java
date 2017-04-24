package io.particle.android.sdk.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.f2prateek.bundler.FragmentBundlerCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static android.content.Context.CLIPBOARD_SERVICE;
import static io.particle.android.sdk.utils.Py.list;

/**
 * Created by Julius.
 */
public class EventsFragment extends Fragment {

    public static EventsFragment newInstance(ParticleDevice device) {
        return FragmentBundlerCompat.create(new EventsFragment())
                .put(EventsFragment.ARG_DEVICE, device)
                .build();
    }

    // The device that this fragment represents
    public static final String ARG_DEVICE = "ARG_DEVICE";

    private ParticleDevice device;
    private Long subscriptionId;
    private boolean subscribed;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_events, container, false);
        device = getArguments().getParcelable(ARG_DEVICE);

        RecyclerView rv = Ui.findView(top, R.id.events_list);
        rv.setHasFixedSize(true);  // perf. optimization
        LinearLayoutManager layoutManager = new LinearLayoutManager(inflater.getContext());
        rv.setLayoutManager(layoutManager);
        EventListAdapter adapter = new EventListAdapter(device);
        rv.setAdapter(adapter);
        rv.addItemDecoration(new DividerItemDecoration(getContext(), LinearLayout.VERTICAL));

        top.findViewById(R.id.events_clear).setOnClickListener(v -> adapter.clear());
        initEventSubscription(top, adapter);
        initFiltering(top, adapter);
        return top;
    }

    private void initFiltering(View top, EventListAdapter adapter) {
        EditText filter = (EditText) top.findViewById(R.id.events_search);
        filter.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                adapter.filter(v.getText().toString());
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
                return true;
            }
            return false;
        });
    }

    private void initEventSubscription(View top, EventListAdapter adapter) {
        ImageView eventButton = (ImageView) top.findViewById(R.id.events_pause);
        eventButton.setOnClickListener(v -> {
            if (subscribed) {
                eventButton.setImageResource(R.drawable.ic_play);
                stopEventSubscription();
            } else {
                eventButton.setImageResource(R.drawable.ic_pause);
                startEventSubscription(adapter);
            }
        });
        startEventSubscription(adapter);
    }

    private void startEventSubscription(EventListAdapter adapter) {
        subscribed = true;
        Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
            @Override
            public Void callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                subscriptionId = device.subscribeToEvents(null, new ParticleEventHandler() {
                    @Override
                    public void onEventError(Exception e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onEvent(String eventName, ParticleEvent particleEvent) {
                        adapter.add(new Event(eventName, particleEvent));
                    }
                });
                return null;
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    private void stopEventSubscription() {
        subscribed = false;
        Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
            @Override
            public Void callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException, IOException {
                device.unsubscribeFromEvents(subscriptionId);
                return null;
            }

            @Override
            public void onFailure(@NonNull ParticleCloudException exception) {
                exception.printStackTrace();
            }
        });
    }

    private static class Event {
        String name;
        ParticleEvent particleEvent;

        Event(String name, ParticleEvent particleEvent) {
            this.name = name;
            this.particleEvent = particleEvent;
        }
    }

    private static class EventListAdapter extends RecyclerView.Adapter<EventListAdapter.ViewHolder> {

        static class ViewHolder extends RecyclerView.ViewHolder {
            final View topLevel;
            final TextView eventName, eventData, eventTime;
            final View copyButton;

            ViewHolder(View itemView) {
                super(itemView);
                topLevel = itemView;
                eventName = Ui.findView(itemView, R.id.event_name);
                eventData = Ui.findView(itemView, R.id.event_data);
                eventTime = Ui.findView(itemView, R.id.event_time);
                copyButton = Ui.findView(itemView, R.id.event_copy);
            }
        }

        private final List<Event> data = list();
        private final List<Event> filteredData = list();
        private String filter = "";
        private ParticleDevice device;

        EventListAdapter(ParticleDevice device) {
            this.device = device;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_events_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Event event = filteredData.get(position);
            holder.eventName.setText(event.name);
            holder.eventData.setText(event.particleEvent.dataPayload);
            holder.eventTime.setText(new SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
                    .format(event.particleEvent.publishedAt));

            holder.copyButton.setOnClickListener(v -> {
                Context context = holder.itemView.getContext();
                ClipboardManager clipboard = (ClipboardManager) context.getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Event data", buildEventClipboardCopy(event));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, R.string.clipboard_copy_event_msg, Toast.LENGTH_SHORT).show();
            });
        }

        private String buildEventClipboardCopy(Event event) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("Event", event.name);
                jsonObject.put("DeviceID", event.particleEvent.deviceId);
                jsonObject.put("Data", event.particleEvent.dataPayload);
                String dateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm::ssZ",
                        Locale.getDefault()).format(event.particleEvent.publishedAt);
                jsonObject.put("Time", dateTime);
                jsonObject.put("TTL", event.particleEvent.timeToLive);
            } catch (JSONException ignore) {
            }
            return jsonObject.toString();
        }

        public void clear() {
            filteredData.clear();
            data.clear();
            notifyDataSetChanged();
        }

        public void add(Event event) {
            data.add(0, event);
            if (event.name.contains(filter)) {
                filteredData.add(0, event);
                notifyItemInserted(0);
            }
        }

        public void filter(String filterText) {
            this.filter = filterText;
            filteredData.clear();
            notifyDataSetChanged();

            for (Event event : data) {
                if (event.name.contains(filter)) {
                    filteredData.add(event);
                    notifyItemInserted(data.indexOf(event));
                }
            }
        }

        @Override
        public int getItemCount() {
            return filteredData.size();
        }
    }
}
