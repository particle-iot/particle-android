package io.particle.android.sdk.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.tinker.TinkerFragment
import io.particle.mesh.common.QATool


internal class InspectorPager(
    fm: FragmentManager,
    device: ParticleDevice
) : FragmentStatePagerAdapter(fm) {

    private val infoFragment: InfoFragment = InfoFragment.newInstance(device)
    private val dataFragment: DataFragment = DataFragment.newInstance(device)
    private val eventsFragment: EventsFragment = EventsFragment.newInstance(device)
    private val tinkerFragment: TinkerFragment = TinkerFragment.newInstance(device)
    private val isRunningTinker = device.isRunningTinker

    override fun getPageTitle(position: Int): CharSequence? {
        return when (position) {
            0 -> "Info"
            1 -> "Data"
            2 -> "Events"
            3 -> "Tinker"
            else -> null
        }
    }

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> infoFragment
            1 -> dataFragment
            2 -> eventsFragment
            3 -> tinkerFragment
            else -> {
                QATool.report(IllegalArgumentException("Invalid position=$position"))
                null
            }
        }
    }

    override fun getCount(): Int {
        return if (isRunningTinker) 4 else 3
    }
}
