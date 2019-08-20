package io.particle.android.sdk.ui

import androidx.collection.SparseArrayCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import io.particle.android.sdk.cloud.ParticleDevice
import io.particle.android.sdk.tinker.TinkerFragment
import io.particle.android.sdk.ui.FunctionsAndVariablesFragment.DisplayMode


private data class FragmentData(val title: String, val fragment: Fragment)


internal class InspectorPager(
    fm: FragmentManager,
    device: ParticleDevice
) : FragmentStatePagerAdapter(fm) {

    // NOTE: the keys in this map are the positions of the fragments.
    private val fragmentsData = SparseArrayCompat<FragmentData>().apply {
        addInGivenOrder(
            "Events" to EventsFragment.newInstance(device),
            "Functions" to FunctionsAndVariablesFragment.newInstance(device, DisplayMode.FUNCTIONS),
            "Variables" to FunctionsAndVariablesFragment.newInstance(device, DisplayMode.VARIABLES),
            "Tinker" to TinkerFragment.newInstance(device)
        )
    }

    override fun getPageTitle(position: Int): CharSequence? {
        return fragmentsData[position]?.title
    }

    override fun getItem(position: Int): Fragment {
        return fragmentsData[position]?.fragment!!
    }

    override fun getCount(): Int {
        return fragmentsData.size()
    }
}


private fun SparseArrayCompat<FragmentData>.addInGivenOrder(vararg pairs: Pair<String, Fragment>) {
    pairs.forEachIndexed { index, element ->
        this.put(index, FragmentData(element.first, element.second))
    }
}
