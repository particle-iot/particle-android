package io.particle.mesh.setup.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.squareup.phrase.Phrase
import io.particle.mesh.R
import kotlinx.android.synthetic.main.fragment_assisting_device_pairing_progress.*


class AssistingDevicePairingProgressFragment : BaseMeshSetupFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(
            R.layout.fragment_assisting_device_pairing_progress,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        status_text.text = Phrase.from(view, R.string.pairing_with_your_device)
            .put("product_type", flowManagerVM.flowManager!!.getTypeName())
            .format()
    }

}
