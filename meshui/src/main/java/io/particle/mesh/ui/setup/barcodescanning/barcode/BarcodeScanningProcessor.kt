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
package io.particle.mesh.ui.setup.barcodescanning.barcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import io.particle.mesh.common.android.livedata.setOnMainThread
import io.particle.mesh.ui.setup.barcodescanning.FrameMetadata
import io.particle.mesh.ui.setup.barcodescanning.GraphicOverlay
import io.particle.mesh.ui.setup.barcodescanning.VisionProcessorBase
import mu.KotlinLogging
import java.io.IOException


class BarcodeScanningProcessor : VisionProcessorBase<List<Barcode>>() {

    val foundBarcodes: LiveData<List<Barcode>>
        get() = mutableFoundBarcodes

    private val mutableFoundBarcodes = MutableLiveData<List<Barcode>>()

    private val detector: BarcodeScanner

    private val log = KotlinLogging.logger {}

    init {
        // Note that if you know which format of barcode your app is dealing with, detection will be
        // faster to specify the supported barcode formats
        val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_DATA_MATRIX)
                .build()
        detector = BarcodeScanning.getClient(options)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            log.error(e) { "Exception thrown while trying to close Barcode Detector" }
        }

    }

    override fun detectInImage(image: InputImage): Task<List<Barcode>> {
        return detector.process(image)
    }

    override fun onSuccess(
            barcodes: List<Barcode>,
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
