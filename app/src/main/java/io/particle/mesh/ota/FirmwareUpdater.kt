package io.particle.mesh.ota

import androidx.annotation.WorkerThread
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import io.particle.firmwareprotos.ctrl.Config.DeviceMode
import io.particle.mesh.bluetooth.connecting.ConnectionPriority
import io.particle.mesh.common.QATool
import io.particle.mesh.common.Result
import io.particle.mesh.setup.connection.ProtocolTransceiver
import mu.KotlinLogging
import okio.Buffer
import java.io.IOException
import java.net.URL
import kotlin.math.min


typealias ProgressListener = (progressPercentage: Int) -> Unit


class FirmwareUpdater(
    private val protocolTransceiver: ProtocolTransceiver,
    private val okHttpClient: OkHttpClient
) {

    private val log = KotlinLogging.logger {}

    @WorkerThread
    suspend fun updateFirmware(url: URL, listener: ProgressListener) {
        val bytes = retrieveUpdate(url)
        updateFirmware(bytes, listener)
    }

    @WorkerThread
    private suspend fun updateFirmware(firmwareData: ByteArray, listener: ProgressListener) {
        try {
            doUpdateFirmware(firmwareData, listener)
        } finally {
            protocolTransceiver.setConnectionPriority(ConnectionPriority.BALANCED)
        }
    }

    @WorkerThread
    private fun retrieveUpdate(url: URL): ByteArray {
        val request = Request.Builder()
            .url(url)
            .get()
            .build()
        val call = okHttpClient.newCall(request)
        val response = call.execute()
        return response.body().bytes()
    }

    private suspend fun doUpdateFirmware(
        firmwareData: ByteArray,
        listener: ProgressListener
    ) {
        showFeedback("Starting firmware update")

        protocolTransceiver.setConnectionPriority(ConnectionPriority.HIGH)

        protocolTransceiver.sendStartupMode(DeviceMode.LISTENING_MODE)

        val startReplyResult = protocolTransceiver.sendStartFirmwareUpdate(firmwareData.size)
        val chunkSize = when (startReplyResult) {
            is Result.Present -> startReplyResult.value.chunkSize
            is Result.Error,
            is Result.Absent -> {
                throw IOException("Bad reply from device: ${startReplyResult.error}")
            }
        }
        showFeedback("Chunk size: $chunkSize")

        val buffer = Buffer()
        buffer.write(firmwareData)

        var bytesSent = 0
        while (!buffer.exhausted()) {
            val toRead = min(buffer.size(), chunkSize.toLong())
            val toSend = buffer.readByteArray(toRead)
            val updateResult = protocolTransceiver.sendFirmwareUpdateData(toSend)

            when (updateResult) {
                is Result.Error,
                is Result.Absent -> throw IOException("Bad reply from device: ${updateResult.error}")
                is Result.Present -> {
                    bytesSent += toRead.toInt()
                    val progress = ((bytesSent.toFloat() / firmwareData.size.toFloat()) * 100).toInt()
                    log.debug { "Sent ${bytesSent / 1024} KB, progress=$progress" }
                    listener(progress)
                }
            }
        }

        val firmwareUpdateReply = protocolTransceiver.sendFinishFirmwareUpdate(false)
        when (firmwareUpdateReply) {
            is Result.Present -> log.debug { "Firmware update completed successfully" }
            is Result.Error,
            is Result.Absent -> {
                val errMsg = "Firmware update failed. Error: '${firmwareUpdateReply.error}'"
                QATool.report(IOException(errMsg))
            }
        }
    }

    private fun showFeedback(msg: String) {
//        ctx.safeToast(msg)
        log.debug { msg }
    }
}