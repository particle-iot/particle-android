package io.particle.mesh.setup.connection


import androidx.annotation.MainThread
import io.particle.mesh.bluetooth.PacketMTUSplitter
import io.particle.mesh.bluetooth.connecting.BluetoothConnection
import io.particle.mesh.common.QATool
import io.particle.mesh.setup.connection.security.SecurityManager
import io.particle.mesh.setup.flow.Scopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


private const val FULL_PROTOCOL_HEADER_SIZE = 6
private const val AES_CCM_MAC_SIZE = 8


class ProtocolTransceiverFactory(
        private val securityManager: SecurityManager
) {

    @MainThread
    suspend fun buildProtocolTransceiver(
            deviceConnection: BluetoothConnection,
            name: String,
            scopes: Scopes,
            jpakeLowEntropyPassword: String
    ): ProtocolTransceiver? {

        val packetMTUSplitter = PacketMTUSplitter({ packet ->
            deviceConnection.packetSendChannel.offer(packet)
        })
        val frameWriter = OutboundFrameWriter { packetMTUSplitter.splitIntoPackets(it) }
        val frameReader = InboundFrameReader()

        scopes.onWorker {
            for (packet in deviceConnection.packetReceiveChannel) {
                QATool.runSafely({ frameReader.receivePacket(BlePacket(packet)) })
            }
        }

        val cryptoDelegate = securityManager.createCryptoDelegate(
                jpakeLowEntropyPassword,
                frameWriter,
                frameReader
        ) ?: return null

        frameReader.cryptoDelegate = cryptoDelegate
        frameWriter.cryptoDelegate = cryptoDelegate

        // now that we've passed the security handshake, set the correct number of bytes to assume
        // for the message headers
        frameReader.extraHeaderBytes = FULL_PROTOCOL_HEADER_SIZE + AES_CCM_MAC_SIZE

        val requestWriter = RequestWriter { frameWriter.writeFrame(it) }
        val requestSender = ProtocolTransceiver(requestWriter, deviceConnection, scopes, name)
        val responseReader = ResponseReader { requestSender.receiveResponse(it) }
        scopes.onWorker  {
            for (inboundFrame in frameReader.inboundFrameChannel) {
                QATool.runSafely({ responseReader.receiveResponseFrame(inboundFrame) })
            }
        }

        return requestSender
    }

}
