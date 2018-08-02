package io.particle.particlemesh

import android.content.Context
import io.particle.particlemesh.bluetooth.connecting.ConnectionPriority
import io.particle.particlemesh.common.Result
import io.particle.particlemesh.meshsetup.RequestSender
import io.particle.particlemesh.meshsetup.utils.safeToast
import mu.KotlinLogging
import okio.Buffer
import java.io.IOException
import kotlin.math.min


class FirmwareUpdater(
        private val requestSender: RequestSender,
        private val ctx: Context
) {

    private val log = KotlinLogging.logger {}

    suspend fun updateFirmware(firmwareData: ByteArray) {
        try {
            doUpdateFirmware(firmwareData)
        } finally {
            requestSender.setConnectionPriority(ConnectionPriority.BALANCED)
        }
    }

    private suspend fun doUpdateFirmware(firmwareData: ByteArray) {
        ctx.safeToast("Starting FW update")

        log.info { "Firmware MD5: ${Buffer().write(firmwareData).md5().hex()}" }

        requestSender.setConnectionPriority(ConnectionPriority.HIGH)

        val startReplyResult = requestSender.sendStartFirmwareUpdate(firmwareData.size)
        val chunkSize = when (startReplyResult) {
            is Result.Present -> startReplyResult.value.chunkSize
            is Result.Error,
            is Result.Absent -> {
                throw IOException("Bad reply from device: ${startReplyResult.error}")
            }
        }
        ctx.safeToast("Chunk size: $chunkSize")

        val buffer = Buffer()
        buffer.write(firmwareData)

        var bytesSent = 0
        while (!buffer.exhausted()) {
            val toRead = min(buffer.size(), chunkSize.toLong())
            val toSend = buffer.readByteArray(toRead)
            val updateResult = requestSender.sendFirmwareUpdateData(toSend)

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

        requestSender.sendFinishFirmwareUpdate(false)
    }

}