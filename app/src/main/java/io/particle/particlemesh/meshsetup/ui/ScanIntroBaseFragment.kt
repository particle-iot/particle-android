package io.particle.particlemesh.meshsetup.ui

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.particle.sdk.app.R
import io.particle.sdk.app.R.layout
import kotlinx.android.synthetic.main.fragment_scan_code_intro.view.*
import mu.KotlinLogging

abstract class ScanIntroBaseFragment : BaseMeshSetupFragment() {

    abstract fun onBarcodeUpdated(barcodeData: BarcodeData?)

    abstract val layoutId: Int


    private lateinit var scanViewModel: ScanViewModel

    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanViewModel = ScanViewModel.getViewModel(requireActivity())
        scanViewModel.latestScannedBarcode.observe(this, Observer { doOnBarcodeUpdated(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(layoutId, container, false)

        root.findViewById<View>(R.id.action_next).setOnClickListener {
            findNavController().navigate(R.id.action_global_scanCodeFragment)
        }

        return root
    }

    override fun onDestroy() {
        super.onDestroy()
        stopObserving()
    }

    private fun doOnBarcodeUpdated(barcodeData: BarcodeData?) {
        log.debug { "onBarcodeUpdated(): '$barcodeData'" }

        if (barcodeData == null) {
            // keep listening
            return
        }

        stopObserving()
        scanViewModel.clearValue()
        onBarcodeUpdated(barcodeData)
    }

    private fun stopObserving() {
        scanViewModel.latestScannedBarcode.removeObservers(this)
    }

}