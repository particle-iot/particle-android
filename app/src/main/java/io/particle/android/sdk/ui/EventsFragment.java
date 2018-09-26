package io.particle.android.sdk.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
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

import butterknife.BindView;
import butterknife.ButterKnife;
import io.particle.android.sdk.cloud.ParticleDevice;
import io.particle.android.sdk.cloud.ParticleEvent;
import io.particle.android.sdk.cloud.ParticleEventHandler;
import io.particle.android.sdk.cloud.exceptions.ParticleCloudException;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.ui.Ui;
import io.particle.sdk.app.R;

import static android.content.Context.CLIPBOARD_SERVICE;
import static io.particle.android.sdk.utils.Py.list;
import static java.util.Objects.requireNonNull;

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

    @BindView(R.id.events_list) RecyclerView eventsRecyclerView;
    @BindView(R.id.events_empty) View emptyView;
    private LinearLayoutManager eventsLayoutManager;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View top = inflater.inflate(R.layout.fragment_events, container, false);
        ButterKnife.bind(this, top);

        device = requireNonNull(getArguments()).getParcelable(ARG_DEVICE);
        emptyView.setVisibility(View.VISIBLE);

        eventsRecyclerView.setHasFixedSize(true);  // perf. optimization
        eventsLayoutManager = new SpeedyLinearLayoutManager(inflater.getContext());
        eventsRecyclerView.setLayoutManager(eventsLayoutManager);
        EventListAdapter adapter = new EventListAdapter();
        eventsRecyclerView.setAdapter(adapter);
        eventsRecyclerView.addItemDecoration(new DividerItemDecoration(requireNonNull(getContext()), LinearLayout.VERTICAL));

        setupClearListener(top, adapter);
        initEventSubscription(top, adapter);
        initFiltering(top, adapter);
        return top;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!subscribed) {
            startEventSubscription((EventListAdapter) eventsRecyclerView.getAdapter());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopEventSubscription();
    }

    private void setupClearListener(View rootView, EventListAdapter adapter) {
        rootView.findViewById(R.id.events_clear).setOnClickListener(v -> new AlertDialog.Builder(requireNonNull(getActivity()))
                .setTitle(R.string.clear_events_title)
                .setMessage(R.string.clear_events_message)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    emptyView.setVisibility(View.VISIBLE);
                    adapter.clear();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show());
    }

    private void initFiltering(View top, EventListAdapter adapter) {
        EditText filter = Ui.findView(top, R.id.events_search);
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
        filter.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                adapter.filter(filter.getText().toString());
            }
        });
    }

    private void initEventSubscription(View top, EventListAdapter adapter) {
        ImageView eventButton = top.findViewById(R.id.events_pause);
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
        try {
            Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
                @Override
                public Void callApi(@NonNull ParticleDevice particleDevice) throws IOException {
                    try {
                        subscriptionId = device.subscribeToEvents(null, new ParticleEventHandler() {
                            @Override
                            public void onEventError(Exception e) {
                                e.printStackTrace();
                            }

                            @Override
                            public void onEvent(String eventName, ParticleEvent particleEvent) {
                                adapter.add(new Event(eventName, particleEvent));
                                if (eventsLayoutManager.findFirstVisibleItemPosition() < 1) {
                                    eventsRecyclerView.smoothScrollToPosition(0);
                                }
                                emptyView.post(() -> emptyView.setVisibility(View.GONE));
                            }
                        });
                    } catch (NullPointerException ex) {
                        //set not subscribed
                        subscribed = false;
                    }
                    return null;
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    exception.printStackTrace();
                }
            });
        } catch (ParticleCloudException e) {
            //set not subscribed
            subscribed = false;
        }
    }

    private void stopEventSubscription() {
        subscribed = false;
        try {
            Async.executeAsync(device, new Async.ApiProcedure<ParticleDevice>() {
                @Override
                public Void callApi(@NonNull ParticleDevice particleDevice) throws ParticleCloudException {
                    try {
                        device.unsubscribeFromEvents(subscriptionId);
                    } catch (NullPointerException ignore) {
                        //set to still subscribed
                        subscribed = true;
                    }
                    return null;
                }

                @Override
                public void onFailure(@NonNull ParticleCloudException exception) {
                    exception.printStackTrace();
                }
            });
        } catch (ParticleCloudException e) {
            //set to still subscribed
            subscribed = true;
        }
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

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_events_list, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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

        void clear() {
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

        void filter(String filterText) {
            this.filter = filterText;
            filteredData.clear();
            notifyDataSetChanged();

            for (Event event : data) {
                if (event.name.contains(filter) || event.particleEvent.dataPayload.contains(filter)) {
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

    private static class SpeedyLinearLayoutManager extends LinearLayoutManager {

        private static final float MILLISECONDS_PER_INCH = 250f;

        public SpeedyLinearLayoutManager(Context context) {
            super(context);
        }

        public SpeedyLinearLayoutManager(Context context, int orientation, boolean reverseLayout) {
            super(context, orientation, reverseLayout);
        }

        public SpeedyLinearLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        @Override
        public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
            final LinearSmoothScroller linearSmoothScroller = new LinearSmoothScroller(recyclerView.getContext()) {

                @Override
                public PointF computeScrollVectorForPosition(int targetPosition) {
                    return SpeedyLinearLayoutManager.this.computeScrollVectorForPosition(targetPosition);
                }

                @Override
                protected float calculateSpeedPerPixel(DisplayMetrics displayMetrics) {
                    return MILLISECONDS_PER_INCH / displayMetrics.densityDpi;
                }
            };

            linearSmoothScroller.setTargetPosition(position);
            startSmoothScroll(linearSmoothScroller);
        }
    }
}
