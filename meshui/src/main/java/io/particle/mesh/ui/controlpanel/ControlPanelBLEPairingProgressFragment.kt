package io.particle.mesh.ui.controlpanel

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.squareup.phrase.Phrase
import io.particle.mesh.setup.flow.toUserFacingName
import io.particle.mesh.ui.R
import mu.KotlinLogging


class ControlPanelBLEPairingProgressFragment : BaseControlPanelFragment() {

    private val log = KotlinLogging.logger {}



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_ble_pairing_progress, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        val deviceType = flowUiListener?.targetDevice?.deviceType
        val typeNameRes = deviceType?.toUserFacingName()!!
        val typeName = getString(typeNameRes)

        val tv = view?.findViewById<TextView>(R.id.status_text)
        tv?.text = Phrase.from(view, R.string.pairing_with_your_device)
            .put("product_type", typeName)
            .format()
    }

}
