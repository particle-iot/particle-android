package io.particle.mesh.ui.setup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.MainThread
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.utils.getViewModel
import mu.KotlinLogging

abstract class ScanIntroBaseFragment : BaseFlowFragment() {

    @MainThread
    abstract fun onBarcodeUpdated(barcodeData: CompleteBarcodeData?)

    abstract val layoutId: Int


    private lateinit var scanViewModel: ScanViewModel

    private val log = KotlinLogging.logger {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        scanViewModel = this.getViewModel()
        scanViewModel.latestScannedBarcode.observe(this, Observer { doOnBarcodeUpdated(it) })
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(layoutId, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)


        view?.findViewById<View>(R.id.action_next)?.setOnClickListener {
            // FIXME: this probably shouldn't be in the UI layer
            findNavController().navigate(R.id.action_global_scanCodeFragment)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopObserving()
    }

    private fun doOnBarcodeUpdated(barcodeData: CompleteBarcodeData?) {
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