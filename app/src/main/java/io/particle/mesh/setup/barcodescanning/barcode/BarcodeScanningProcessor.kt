// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package io.particle.mesh.setup.barcodescanning.barcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions.Builder
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.setup.barcodescanning.FrameMetadata
import io.particle.mesh.setup.barcodescanning.GraphicOverlay
import io.particle.mesh.setup.barcodescanning.VisionProcessorBase
import mu.KotlinLogging
import java.io.IOException


class BarcodeScanningProcessor : VisionProcessorBase<List<FirebaseVisionBarcode>>() {

    val foundBarcodes: LiveData<List<FirebaseVisionBarcode>>
        get() = mutableFoundBarcodes

    private val mutableFoundBarcodes = MutableLiveData<List<FirebaseVisionBarcode>>()

    private val detector: FirebaseVisionBarcodeDetector

    private val log = KotlinLogging.logger {}

    init {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats
        val options = Builder()
                .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_DATA_MATRIX)
                .build()
        detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            log.error(e) { "Exception thrown while trying to close Barcode Detector" }
        }

    }

    override fun detectInImage(image: FirebaseVisionImage): Task<List<FirebaseVisionBarcode>> {
        return detector.detectInImage(image)
    }

    override fun onSuccess(
            barcodes: List<FirebaseVisionBarcode>,
            frameMetadata: FrameMetadata,
            graphicOverlay: GraphicOverlay) {

        graphicOverlay.clear()
        for (i in barcodes.indices) {
//            val barcode = barcodes[i]
//            val barcodeGraphic = BarcodeGraphic(graphicOverlay, barcode)
//            graphicOverlay.add(barcodeGraphic)
        }

        mutableFoundBarcodes.setOnMainThread(barcodes)
    }

    override fun onFailure(e: Exception) {
        log.error(e) { "Barcode detection failed" }
    }
}
