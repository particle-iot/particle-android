package io.particle.mesh.setup.connection


import android.support.annotation.MainThread
import android.support.v4.util.SparseArrayCompat
import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.particle.firmwareprotos.ctrl.Common
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.firmwareprotos.ctrl.Config.*
import io.particle.firmwareprotos.ctrl.Extensions
import io.particle.firmwareprotos.ctrl.StorageOuterClass.*
import io.particle.firmwareprotos.ctrl.mesh.Mesh.*
import io.particle.mesh.bluetooth.PacketMTUSplitter
import io.particle.mesh.bluetooth.connecting.ConnectionPriority
import io.particle.mesh.bluetooth.connecting.BluetoothConnection
import io.particle.mesh.common.QATool
import io.particle.mesh.common.Result
import io.particle.mesh.setup.connection.security.CryptoDelegateFactory
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


private const val FULL_PROTOCOL_HEADER_SIZE = 6
private const val AES_CCM_MAC_SIZE = 8

private var requestIdGenerator = AtomicInteger()


private fun generateRequestId(): Short {
    return requestIdGenerator.incrementAndGet().toShort()
}


internal fun AbstractMessage.asRequest(): DeviceRequest {
    return DeviceRequest(
            generateRequestId(),
            // get type ID from the message descriptor
            this.descriptorForType.options.getExtension(Extensions.typeId).toShort(),
            this.toByteArray()
    )
}


// FIXME: rewrite using descriptors
private fun Int.toResultCode(): Common.ResultCode {
    return when (this) {
        0 -> Common.ResultCode.OK
        -130 -> Common.ResultCode.NOT_ALLOWED
        -160 -> Common.ResultCode.TIMEOUT
        -170 -> Common.ResultCode.NOT_FOUND
        -180 -> Common.ResultCode.ALREADY_EXIST
        -210 -> Common.ResultCode.INVALID_STATE
        -260 -> Common.ResultCode.NO_MEMORY
        -270 -> Common.ResultCode.INVALID_PARAM
        else -> throw IllegalArgumentException("Invalid value for ResultCode: $this")
    }
}


class ProtocolTransceiverFactory(
        private val cryptoDelegateFactory: CryptoDelegateFactory
) {

    @MainThread
    suspend fun buildProtocolTransceiver(
            deviceConnection: BluetoothConnection,
            name: String,
            jpakeLowEntropyPassword: String
    ): ProtocolTransceiver? {

        val packetMTUSplitter = PacketMTUSplitter({ packet ->
            deviceConnection.packetSendChannel.offer(packet)
        })
        val frameWriter = OutboundFrameWriter { packetMTUSplitter.splitIntoPackets(it) }
        val frameReader = InboundFrameReader()
        launch {
            for (packet in deviceConnection.packetReceiveChannel) {
                QATool.runSafely({ frameReader.receivePacket(BlePacket(packet)) })
            }
        }

        val cryptoDelegate = cryptoDelegateFactory.createCryptoDelegate(
                frameWriter,
                frameReader,
                jpakeLowEntropyPassword
        ) ?: return null

        frameReader.cryptoDelegate = cryptoDelegate
        frameWriter.cryptoDelegate = cryptoDelegate

        // now that we've passed the security handshake, set the correct number of bytes to assume
        // for the message headers
        frameReader.extraHeaderBytes = FULL_PROTOCOL_HEADER_SIZE + AES_CCM_MAC_SIZE

        val requestWriter = RequestWriter { frameWriter.writeFrame(it) }
        val requestSender = ProtocolTransceiver(requestWriter, deviceConnection, name)
        val responseReader = ResponseReader { requestSender.receiveResponse(it) }
        launch {
            for (inboundFrame in frameReader.inboundFrameChannel) {
                QATool.runSafely({ responseReader.receiveResponseFrame(inboundFrame) })
            }
        }

        return requestSender
    }

}


class ProtocolTransceiver internal constructor(
        private val requestWriter: RequestWriter,
        private val connection: BluetoothConnection,
        private val connectionName: String
) {

    private val log = KotlinLogging.logger {}
    private val requestCallbacks = SparseArrayCompat<(DeviceResponse?) -> Unit>()

    val isConnected: Boolean
        get() = connection.isConnected

    val deviceName: String
        get() = connection.deviceName

    fun disconnect() {
        connection.disconnect()
    }

    fun setConnectionPriority(priority: ConnectionPriority) {
        connection.setConnectionPriority(priority)
    }

    suspend fun sendStopListeningMode(): Result<StopListeningModeReply, ResultCode> {
        val response = sendRequest(StopListeningModeRequest.newBuilder().build())
        return buildResult(response) { r -> StopListeningModeReply.parseFrom(r.payloadData) }
    }

    suspend fun sendSetDeviceSetupDone(): Result<SetDeviceSetupDoneReply, ResultCode> {
        val response = sendRequest(SetDeviceSetupDoneRequest.newBuilder().build())
        return buildResult(response) { r -> SetDeviceSetupDoneReply.parseFrom(r.payloadData) }
    }

    suspend fun sendStartFirmwareUpdate(firmwareSizeBytes: Int
    ): Result<StartFirmwareUpdateReply, Common.ResultCode> {
        val response = sendRequest(
                StartFirmwareUpdateRequest.newBuilder()
                        .setSize(firmwareSizeBytes)
                        .build()
        )
        return buildResult(response) { r -> StartFirmwareUpdateReply.parseFrom(r.payloadData) }
    }

    suspend fun sendFirmwareUpdateData(chunk: ByteArray): Result<FirmwareUpdateDataReply, Common.ResultCode> {
        val response = sendRequest(
                FirmwareUpdateDataRequest.newBuilder()
                        .setData(ByteString.copyFrom(chunk))
                        .build()
        )
        return buildResult(response) { r -> FirmwareUpdateDataReply.parseFrom(r.payloadData) }
    }

    suspend fun sendFinishFirmwareUpdate(validateOnly: Boolean
    ): Result<FinishFirmwareUpdateReply, Common.ResultCode> {
        val response = sendRequest(
                FinishFirmwareUpdateRequest.newBuilder()
                        .setValidateOnly(validateOnly)
                        .build()
        )
        return buildResult(response) { r -> FinishFirmwareUpdateReply.parseFrom(r.payloadData) }
    }

    suspend fun sendGetNetworkInfo(): Result<GetNetworkInfoReply, Common.ResultCode> {
        val response = sendRequest(
                GetNetworkInfoRequest.newBuilder()
                        .build()
        )
        return buildResult(response) { r -> GetNetworkInfoReply.parseFrom(r.payloadData) }
    }

    suspend fun sendCreateNetwork(name: String, password: String
    ): Result<CreateNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                CreateNetworkRequest.newBuilder()
                        .setName(name)
                        .setPassword(password)
                        .build()
        )
        return buildResult(response) { r -> CreateNetworkReply.parseFrom(r.payloadData) }
    }

    suspend fun sendAuth(commissionerCredential: String): Result<AuthReply, Common.ResultCode> {
        val response = sendRequest(
                AuthRequest.newBuilder()
                        .setPassword(commissionerCredential)
                        .build()
        )
        return buildResult(response) { r -> AuthReply.parseFrom(r.payloadData) }
    }

    suspend fun sendStartCommissioner(): Result<StartCommissionerReply, Common.ResultCode> {
        val response = sendRequest(StartCommissionerRequest.newBuilder().build())
        return buildResult(response) { r -> StartCommissionerReply.parseFrom(r.payloadData) }
    }

    suspend fun sendPrepareJoiner(network: NetworkInfo): Result<PrepareJoinerReply, Common.ResultCode> {
        val response = sendRequest(
                PrepareJoinerRequest.newBuilder()
                        .setNetwork(network)
                        .build()
        )
        return buildResult(response) { r -> PrepareJoinerReply.parseFrom(r.payloadData) }
    }

    suspend fun sendAddJoiner(eui64: String,
                              joiningCredential: String
    ): Result<AddJoinerReply, Common.ResultCode> {
        val response = sendRequest(
                AddJoinerRequest.newBuilder()
                        .setEui64(eui64)
                        .setPassword(joiningCredential)
                        .build()
        )
        return buildResult(response) { r -> AddJoinerReply.parseFrom(r.payloadData) }
    }

    suspend fun sendJoinNetwork(): Result<JoinNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                JoinNetworkRequest.newBuilder().build(),
                // NOTE: yes, 25 seconds is a crazy timeout, but this is how long it takes to receive
                // a response sometimes.
                25000
        )
        return buildResult(response) { r -> JoinNetworkReply.parseFrom(r.payloadData) }
    }

    suspend fun sendStopCommissioner(): Result<StopCommissionerReply, Common.ResultCode> {
        val response = sendRequest(StopCommissionerRequest.newBuilder().build()
        )
        return buildResult(response) { r -> StopCommissionerReply.parseFrom(r.payloadData) }
    }

    suspend fun sendLeaveNetwork(): Result<LeaveNetworkReply, Common.ResultCode> {
        val response = sendRequest(LeaveNetworkRequest.newBuilder().build())
        return buildResult(response) { r -> LeaveNetworkReply.parseFrom(r.payloadData) }
    }

    suspend fun sendGetDeviceId(): Result<GetDeviceIdReply, ResultCode> {
        val response = sendRequest(GetDeviceIdRequest.newBuilder().build())
        return buildResult(response) { r -> GetDeviceIdReply.parseFrom(r.payloadData) }
    }

    suspend fun sendSetClaimCode(claimCode: String): Result<SetClaimCodeReply, ResultCode> {
        val response = sendRequest(
                SetClaimCodeRequest.newBuilder()
                        .setCode(claimCode)
                        .build()
        )
        return buildResult(response) { r -> SetClaimCodeReply.parseFrom(r.payloadData) }
    }

    suspend fun sendScanNetworks(): Result<ScanNetworksReply, ResultCode> {
        val response = sendRequest(ScanNetworksRequest.newBuilder().build())
        return buildResult(response) { r -> ScanNetworksReply.parseFrom(r.payloadData) }
    }

    suspend fun sendGetSerialNumber(): Result<GetSerialNumberReply, ResultCode> {
        val response = sendRequest(GetSerialNumberRequest.newBuilder().build())
        return buildResult(response) { r -> GetSerialNumberReply.parseFrom(r.payloadData) }
    }

    fun receiveResponse(responseFrame: DeviceResponse) {
        val callback = requestCallbacks[responseFrame.requestId.toInt()]
        if (callback != null) {
            callback(responseFrame)
        } else {
            // FIXME: handle the timeout case here better
            QATool.report(IllegalStateException("No callbacks found for request! ID: ${responseFrame.requestId}"))
            log.error { "No callbacks found for request! ID: ${responseFrame.requestId}" }
        }
    }

    // FIXME: timeouts don't appear to be working as intended

    //region PRIVATE
    private suspend fun sendRequest(
            message: GeneratedMessageV3,
            timeout: Int = BLE_PROTO_REQUEST_TIMEOUT_MILLIS
    ): DeviceResponse? {
        log.info { "Sending message ${message.javaClass} to $connectionName: '$message'" }
        val requestFrame = message.asRequest()
        val response = withTimeoutOrNull(timeout) {
            suspendCoroutine { continuation: Continuation<DeviceResponse?> ->
                doSendRequest(requestFrame) { continuation.resume(it) }
            }
        }

        val id = requestFrame.requestId.toInt()
        if (response == null) {
            log.warn { "Timeout reached for request $id" }
        }
        // By the time we get to here, our callback has been used up,
        // so we can remove it from the map.
        requestCallbacks.remove(id)
        return response
    }

    private fun doSendRequest(
            request: DeviceRequest,
            continuationCallback: (DeviceResponse?) -> Unit) {
        val requestCallback = { frame: DeviceResponse? -> continuationCallback(frame) }
        requestCallbacks.put(request.requestId.toInt(), requestCallback)
        requestWriter.writeRequest(request)
    }

    private fun <V : GeneratedMessageV3> buildResult(
            response: DeviceResponse?,
            successTransformer: (DeviceResponse) -> V
    ): Result<V, Common.ResultCode> {
        if (response == null) {
            return Result.Absent()
        }

        return if (response.resultCode == 0) {
            val transformed = successTransformer(response)
            log.info { "Successful response ${transformed::class.java}: '$transformed'" }
            Result.Present(transformed)
        } else {
            val code = response.resultCode.toResultCode()
            log.error { "Error with request/response: error code $code" }
            Result.Error(code)
        }
    }
    //endregion
}
