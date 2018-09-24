package io.particle.android.sdk.ui;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import io.particle.android.sdk.cloud.ParticleDevice;

class InspectorPager extends FragmentStatePagerAdapter {
    private final InfoFragment infoFragment;
    private final DataFragment dataFragment;
    private final EventsFragment eventsFragment;

    InspectorPager(FragmentManager fm, ParticleDevice device) {
        super(fm);
        infoFragment = InfoFragment.newInstance(device);
        dataFragment = DataFragment.newInstance(device);
        eventsFragment = EventsFragment.newInstance(device);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Info";
            case 1:
                return "Data";
            case 2:
                return "Events";
            default:
                return null;
        }
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return infoFragment;
            case 1:
                return dataFragment;
            case 2:
                return eventsFragment;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 3;
    }
}
