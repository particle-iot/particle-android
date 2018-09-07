package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_check_ethernet_gateway.view.*


class CheckEthernetGatewayFragment : BaseMeshSetupFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_check_ethernet_gateway, container, false)

        root.action_next.setOnClickListener {
            findNavController().navigate(
                    R.id.action_checkEthernetGatewayFragment_to_connectingToDeviceCloudFragment
            )
        }


        return root
    }

}
