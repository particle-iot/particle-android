package io.particle.particlemesh.meshsetup.ui


import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.Navigation

import io.particle.particlemesh.R
import kotlinx.android.synthetic.main.fragment_scan_code_intro.view.*


class ScanCodeIntroFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_scan_code_intro, container, false)

        // FIXME: implement QR scanning
//        root.action_next.setOnClickListener(Navigation.createNavigateOnClickListener(
//                R.id.action_scanCodeIntroFragment_to_scanCodeFragment
//        ))
        root.action_type_serial_number.setOnClickListener(Navigation.createNavigateOnClickListener(
                R.id.action_scanCodeIntroFragment_to_typeCodeManuallyFragment
        ))

        // FIXME: REMOVE
        root.action_next.setOnClickListener(Navigation.createNavigateOnClickListener(
                R.id.action_scanCodeIntroFragment_to_typeCodeManuallyFragment
        ))


        return root
    }

}
