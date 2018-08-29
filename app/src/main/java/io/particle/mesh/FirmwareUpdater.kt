package io.particle.mesh

import android.content.Context
import io.particle.mesh.bluetooth.connecting.ConnectionPriority
import io.particle.mesh.common.Result
import io.particle.mesh.setup.connection.ProtocolTranceiver
import io.particle.mesh.setup.utils.safeToast
import mu.KotlinLogging
import okio.Buffer
import java.io.IOException
import kotlin.math.min


class FirmwareUpdater(
        private val protocolTranceiver: ProtocolTranceiver,
        private val ctx: Context
) {

    private val log = KotlinLogging.logger {}

    suspend fun updateFirmware(firmwareData: ByteArray) {
        try {
            doUpdateFirmware(firmwareData)
        } finally {
            protocolTranceiver.setConnectionPriority(ConnectionPriority.BALANCED)
        }
    }

    private suspend fun doUpdateFirmware(firmwareData: ByteArray) {
        showFeedback("Starting firmware update")

        protocolTranceiver.setConnectionPriority(ConnectionPriority.HIGH)

        val startReplyResult = protocolTranceiver.sendStartFirmwareUpdate(firmwareData.size)
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
            val updateResult = protocolTranceiver.sendFirmwareUpdateData(toSend)

            when (updateResult) {
                is Result.Error,
                is Result.Absent -> throw IOException("Bad reply from device: ${updateResult.error}")
                is Result.Present -> {
                    bytesSent += bytesSent + toRead.toInt()
                    if (bytesSent % (10 * 1024) == 0) {
                        log.debug { "Sent ${bytesSent / 1024} KB" }
                    }
                }
            }
        }

        val firmwareUpdateReply = protocolTranceiver.sendFinishFirmwareUpdate(false)
        val msg = when(firmwareUpdateReply) {
            is Result.Present -> "Firmware update completed successfully"
            is Result.Error,
            is Result.Absent -> "Firmware update failed. Error: '${firmwareUpdateReply.error}'"
        }
        showFeedback(msg)
    }

    private fun showFeedback(msg: String) {
        ctx.safeToast(msg)
        log.info { msg }
    }
}