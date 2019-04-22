package io.particle.android.sdk.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

import io.particle.android.sdk.cloud.ParticleDevice


internal class InspectorPager(fm: FragmentManager, device: ParticleDevice) :
    FragmentStatePagerAdapter(fm) {
    private val infoFragment: InfoFragment
    private val dataFragment: DataFragment
    private val eventsFragment: EventsFragment

    init {
        infoFragment = InfoFragment.newInstance(device)
        dataFragment = DataFragment.newInstance(device)
        eventsFragment = EventsFragment.newInstance(device)
    }

    override fun getPageTitle(position: Int): CharSequence? {
        when (position) {
            0 -> return "Info"
            1 -> return "Data"
            2 -> return "Events"
            else -> return null
        }
    }

    override fun getItem(position: Int): Fragment? {
        when (position) {
            0 -> return infoFragment
            1 -> return dataFragment
            2 -> return eventsFragment
            else -> return null
        }
    }

    override fun getCount(): Int {
        return 3
    }
}
