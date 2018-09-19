package io.particle.mesh.setup.ui


import android.Manifest
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat.OnRequestPermissionsResultCallback
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.barcodescanning.CameraSource
import io.particle.mesh.setup.barcodescanning.CameraSourcePreview
import io.particle.mesh.setup.barcodescanning.GraphicOverlay
import io.particle.mesh.setup.barcodescanning.barcode.BarcodeScanningProcessor
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.fragment_scan_code.view.*
import mu.KotlinLogging
import java.io.IOException
import java.util.*


private const val PERMISSION_REQUESTS = 1


class ScanViewModel : ViewModel() {

    companion object {
        fun getViewModel(activity: FragmentActivity): ScanViewModel {
            return ViewModelProviders.of(activity).get(ScanViewModel::class.java)
        }
    }

    val latestScannedBarcode: LiveData<BarcodeData>
        get() = mutableLD

    private val mutableLD = MutableLiveData<BarcodeData>()

    fun updateBarcode(newBarcode: BarcodeData) {
        mutableLD.postValue(newBarcode)
    }

    fun clearValue() {
        mutableLD.postValue(null)
    }

}


data class BarcodeData(
        val serialNumber: String,
        val mobileSecret: String
) {

    companion object {

        fun fromRawData(rawBarcodeData: String?): BarcodeData? {
            if (rawBarcodeData?.length != 31) {
                return null
            }
            val split: List<String> = rawBarcodeData.split(" ")
            if (split.size != 2 || split[0].length != 15 || split[1].length != 15) {
                return null
            }
            return BarcodeData(split[0], split[1])
        }
    }
}


class ScanCodeFragment : BaseMeshSetupFragment(), OnRequestPermissionsResultCallback {

    private lateinit var cloud: ParticleCloud
    private lateinit var preview: CameraSourcePreview
    private lateinit var graphicOverlay: GraphicOverlay
    private lateinit var barcodeScanningProcessor: BarcodeScanningProcessor
    private lateinit var scanViewModel: ScanViewModel

    private val barcodeObserver = Observer<List<FirebaseVisionBarcode>> { onBarcodesScanned(it) }


    private var cameraSource: CameraSource? = null

    private val log = KotlinLogging.logger {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloud = ParticleCloudSDK.getCloud()
        scanViewModel = ScanViewModel.getViewModel(requireActivity())

        barcodeScanningProcessor = BarcodeScanningProcessor()
        barcodeScanningProcessor.foundBarcodes.observe(this, barcodeObserver)
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_scan_code, container, false)

        preview = root.findViewById(R.id.firePreview)
        graphicOverlay = root.findViewById(R.id.fireFaceOverlay)
        root.action_cancel.setOnClickListener{ requireActivity().finish() }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (allPermissionsGranted()) {
            createCameraSource()
        } else {
            getRuntimePermissions()
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            startCameraSource()
        }
    }

    /**
     * Stops the camera.
     */
    override fun onPause() {
        super.onPause()
        preview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource!!.release()
        }
    }

    private fun onBarcodesScanned(foundBarcodes: List<FirebaseVisionBarcode>?) {
        if (foundBarcodes == null) {
            return
        }

        for (bcode in foundBarcodes) {
            val barcode = BarcodeData.fromRawData(bcode.rawValue)
            if (barcode != null) {
                onBarcodeFound(barcode)
                findNavController().popBackStack()
                return
            }
        }
    }

    private fun onBarcodeFound(barcodeData: BarcodeData) {
        barcodeScanningProcessor.foundBarcodes.removeObserver(barcodeObserver)
        scanViewModel.updateBarcode(barcodeData)
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), graphicOverlay)
        }
        cameraSource!!.setFacing(CameraSource.CAMERA_FACING_BACK)
        cameraSource!!.setMachineLearningFrameProcessor(barcodeScanningProcessor)
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private fun startCameraSource() {
        if (cameraSource != null) {
            try {
                preview.start(cameraSource, graphicOverlay)
            } catch (e: IOException) {
                QATool.report(e)
                cameraSource!!.release()
                cameraSource = null
            }
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return arrayOf(Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (!isPermissionGranted(requireContext(), permission)) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions = ArrayList<String>()
        for (permission in getRequiredPermissions()) {
            if (!isPermissionGranted(requireContext(), permission)) {
                allNeededPermissions.add(permission)
            }
        }

        if (!allNeededPermissions.isEmpty()) {
            requestPermissions(allNeededPermissions.toTypedArray(), PERMISSION_REQUESTS)
        }
    }

    override fun onRequestPermissionsResult(
            requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        log.info {"Permission granted!" }
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
            log.info {"Permission is granted: $permission" }
            return true
        }
        log.info {"Permission is NOT granted: $permission" }
        return false
    }

}
