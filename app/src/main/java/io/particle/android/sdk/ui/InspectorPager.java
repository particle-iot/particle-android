package io.particle.android.sdk.ui;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import io.particle.android.sdk.cloud.ParticleDevice;

/**
 * Created by Julius.
 */
public class InspectorPager extends FragmentStatePagerAdapter {
    private InfoFragment infoFragment;
    private DataFragment dataFragment;
    private EventsFragment eventsFragment;

    public InspectorPager(FragmentManager fm, ParticleDevice device) {
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
