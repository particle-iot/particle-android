package io.particle.particlemesh.meshsetup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_scan_code_intro.view.*


class ScanCodeIntroFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_scan_code_intro, container, false)

//        root.action_type_serial_number.setOnClickListener(Navigation.createNavigateOnClickListener(
//                R.id.action_scanCodeIntroFragment_to_typeSerialNumberManuallyFragment
//        ))
        root.action_next.setOnClickListener {
            findNavController().navigate(R.id.action_scanCodeIntroFragment_to_scanCodeFragment)
        }

        return root
    }

}
