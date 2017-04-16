package io.particle.android.sdk.ui;

import android.support.v4.app.Fragment;

import com.f2prateek.bundler.FragmentBundlerCompat;

import io.particle.android.sdk.cloud.ParticleDevice;

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

}
