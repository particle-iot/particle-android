package io.particle.mesh.ui.setup


import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.navigation.fragment.findNavController
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import io.particle.android.sdk.cloud.ParticleCloud
import io.particle.android.sdk.cloud.ParticleCloudSDK
import io.particle.android.sdk.utils.appHasPermission
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.BarcodeData
import io.particle.mesh.setup.BarcodeData.CompleteBarcodeData
import io.particle.mesh.setup.BarcodeData.PartialBarcodeData
import io.particle.mesh.setup.flow.FlowRunnerUiListener
import io.particle.mesh.ui.BaseFlowFragment
import io.particle.mesh.ui.R
import io.particle.mesh.ui.setup.barcodescanning.CameraSource
import io.particle.mesh.ui.setup.barcodescanning.barcode.BarcodeScanningProcessor
import io.particle.mesh.ui.utils.getViewModel
import kotlinx.android.synthetic.main.fragment_scan_code.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.IOException
import java.util.*


private const val PERMISSION_REQUESTS = 1


class ScanViewModel : ViewModel() {

    val latestScannedBarcode: LiveData<CompleteBarcodeData>
        get() = mutableLD

    private val mutableLD = MutableLiveData<CompleteBarcodeData>()

    fun updateBarcode(newBarcode: CompleteBarcodeData) {
        mutableLD.postValue(newBarcode)
    }

    fun clearValue() {
        mutableLD.postValue(null)
    }

}


class ScanCodeFragment : BaseFlowFragment(), OnRequestPermissionsResultCallback {

    private lateinit var cloud: ParticleCloud
    private lateinit var barcodeScanningProcessor: BarcodeScanningProcessor
    private lateinit var scanViewModel: ScanViewModel

    private val barcodeObserver = Observer<List<FirebaseVisionBarcode>> { onBarcodesScanned(it) }

    private var isFetchingCompleteBarcode = false

    private var cameraSource: CameraSource? = null

    private val log = KotlinLogging.logger {}


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cloud = ParticleCloudSDK.getCloud()
        scanViewModel = this.getViewModel()

        barcodeScanningProcessor = BarcodeScanningProcessor()
        barcodeScanningProcessor.foundBarcodes.observe(this, barcodeObserver)
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_scan_code, container, false)
    }

    override fun onFragmentReady(activity: FragmentActivity, flowUiListener: FlowRunnerUiListener) {
        super.onFragmentReady(activity, flowUiListener)

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
        scanPreview.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (cameraSource != null) {
            cameraSource!!.release()
        }
    }

    private fun onBarcodesScanned(foundBarcodes: List<FirebaseVisionBarcode>?) {
        if (foundBarcodes == null || isFetchingCompleteBarcode) {
            return
        }

        for (bcode in foundBarcodes) {
            val barcode =
                BarcodeData.fromRawData(bcode.rawValue)
            if (barcode != null) {
                onBarcodeFound(barcode)
                return
            }
        }
    }

    private fun onBarcodeFound(barcodeData: BarcodeData) {
        when (barcodeData) {
            is CompleteBarcodeData -> onCompleteBarcode(barcodeData)
            is PartialBarcodeData -> onPartialSecretBarcode(barcodeData)
        }
    }

    private fun onCompleteBarcode(barcodeData: CompleteBarcodeData) {
        barcodeScanningProcessor.foundBarcodes.removeObserver(barcodeObserver)
        scanViewModel.updateBarcode(barcodeData)
        findNavController().popBackStack()
    }

    private fun onPartialSecretBarcode(barcodeData: PartialBarcodeData) {
        isFetchingCompleteBarcode = true

        suspend fun onUnableToFetchFullBarcode() {
            withContext(Dispatchers.Main) {
                showBadBarcodeSupportDialog(barcodeData)
                findNavController().popBackStack()
            }
        }

        GlobalScope.launch {
            try {
                val secretResponse = cloud.getFullMobileSecret(
                    barcodeData.serialNumber.value,
                    barcodeData.partialMobileSecret
                )

                if (secretResponse.fullMobileSecret == null) {
                    onUnableToFetchFullBarcode()

                } else {

                    val fullBarcode = CompleteBarcodeData(
                        barcodeData.serialNumber,
                        secretResponse.fullMobileSecret!!
                    )
                    withContext(Dispatchers.Main) { onCompleteBarcode(fullBarcode) }
                }

            } catch (ex: Exception) {
                onUnableToFetchFullBarcode()
            }
        }
    }

    private fun createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = CameraSource(requireActivity(), scanPreviewOverlay)
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
                scanPreview.start(cameraSource, scanPreviewOverlay)
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

        if (allNeededPermissions.isNotEmpty()) {
            requestPermissions(allNeededPermissions.toTypedArray(),
                PERMISSION_REQUESTS
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        log.info { "Permission granted!" }
        if (allPermissionsGranted()) {
            createCameraSource()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun isPermissionGranted(context: Context, permission: String): Boolean {
        if (context.appHasPermission(permission)) {
            log.info { "Permission is granted: $permission" }
            return true
        }
        log.info { "Permission is NOT granted: $permission" }
        return false
    }

    private fun showBadBarcodeSupportDialog(badBarcode: PartialBarcodeData) {
        val activity = requireActivity()

        val emailContent = """


-- BEFORE SENDING Please attach a picture of the device sticker! --


Hi Particle! My sticker barcode has an issue.

Serial number: ${badBarcode.serialNumber}
Full scan results: ${badBarcode.serialNumber} ${badBarcode.partialMobileSecret}


"""

        fun sendSupportEmail() {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf("hello@particle.io"))
                putExtra(Intent.EXTRA_SUBJECT, "3rd generation sticker problem")
                putExtra(Intent.EXTRA_TEXT, emailContent)
            }
            activity.startActivity(emailIntent)
        }


        MaterialDialog.Builder(requireContext())
            .content(R.string.p_sticker_error_dialog_content)
            .positiveText(R.string.p_action_contact_support)
            .onPositive { _, _ ->
                sendSupportEmail()
                flowRunner.endSetup()
            }
            .build()
            .show()
    }

}
