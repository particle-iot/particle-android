package io.particle.particlemesh.meshsetup.ui

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation
import io.particle.particlemesh.R
import kotlinx.android.synthetic.main.fragment_select_device.view.*

class SelectDeviceFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_select_device, container, false)

        root.xenon_row.setOnClickListener(Navigation.createNavigateOnClickListener(
                R.id.action_selectDeviceFragment_to_getReadyForSetupFragment))

        return root
    }

}
