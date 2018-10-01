package io.particle.mesh.setup.connection


import android.util.SparseArray
import androidx.annotation.MainThread
import androidx.collection.SparseArrayCompat
import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.particle.firmwareprotos.ctrl.Common
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.firmwareprotos.ctrl.Config.GetDeviceIdReply
import io.particle.firmwareprotos.ctrl.Config.GetDeviceIdRequest
import io.particle.firmwareprotos.ctrl.Config.GetSerialNumberReply
import io.particle.firmwareprotos.ctrl.Config.GetSerialNumberRequest
import io.particle.firmwareprotos.ctrl.Config.SetClaimCodeReply
import io.particle.firmwareprotos.ctrl.Config.SetClaimCodeRequest
import io.particle.firmwareprotos.ctrl.Config.SetDeviceSetupDoneReply
import io.particle.firmwareprotos.ctrl.Config.SetDeviceSetupDoneRequest
import io.particle.firmwareprotos.ctrl.Config.StartListeningModeReply
import io.particle.firmwareprotos.ctrl.Config.StartListeningModeRequest
import io.particle.firmwareprotos.ctrl.Config.StopListeningModeReply
import io.particle.firmwareprotos.ctrl.Config.StopListeningModeRequest
import io.particle.firmwareprotos.ctrl.Extensions
import io.particle.firmwareprotos.ctrl.Network.GetInterfaceListReply
import io.particle.firmwareprotos.ctrl.Network.GetInterfaceListRequest
import io.particle.firmwareprotos.ctrl.Network.GetInterfaceReply
import io.particle.firmwareprotos.ctrl.Network.GetInterfaceRequest
import io.particle.firmwareprotos.ctrl.Network.NetworkGetConfigurationReply
import io.particle.firmwareprotos.ctrl.Network.NetworkGetConfigurationRequest
import io.particle.firmwareprotos.ctrl.Network.NetworkGetStatusReply
import io.particle.firmwareprotos.ctrl.Network.NetworkGetStatusRequest
import io.particle.firmwareprotos.ctrl.StorageOuterClass.FinishFirmwareUpdateReply
import io.particle.firmwareprotos.ctrl.StorageOuterClass.FinishFirmwareUpdateRequest
import io.particle.firmwareprotos.ctrl.StorageOuterClass.FirmwareUpdateDataReply
import io.particle.firmwareprotos.ctrl.StorageOuterClass.FirmwareUpdateDataRequest
import io.particle.firmwareprotos.ctrl.StorageOuterClass.StartFirmwareUpdateReply
import io.particle.firmwareprotos.ctrl.StorageOuterClass.StartFirmwareUpdateRequest
import io.particle.firmwareprotos.ctrl.cloud.Cloud.GetConnectionStatusReply
import io.particle.firmwareprotos.ctrl.cloud.Cloud.GetConnectionStatusRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.AddJoinerReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.AddJoinerRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.AuthReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.AuthRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.CreateNetworkReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.CreateNetworkRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.GetNetworkInfoReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.GetNetworkInfoRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.JoinNetworkReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.JoinNetworkRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.LeaveNetworkReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.LeaveNetworkRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.NetworkInfo
import io.particle.firmwareprotos.ctrl.mesh.Mesh.PrepareJoinerReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.PrepareJoinerRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.ScanNetworksReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.ScanNetworksRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.StartCommissionerReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.StartCommissionerRequest
import io.particle.firmwareprotos.ctrl.mesh.Mesh.StopCommissionerReply
import io.particle.firmwareprotos.ctrl.mesh.Mesh.StopCommissionerRequest
import io.particle.mesh.bluetooth.PacketMTUSplitter
import io.particle.mesh.bluetooth.connecting.BluetoothConnection
import io.particle.mesh.bluetooth.connecting.ConnectionPriority
import io.particle.mesh.common.QATool
import io.particle.mesh.common.Result
import io.particle.mesh.setup.connection.security.SecurityManager
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.android.UI
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine


private const val FULL_PROTOCOL_HEADER_SIZE = 6
private const val AES_CCM_MAC_SIZE = 8
private const val DEFAULT_NETWORK_CHANNEL = 11

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
        private val securityManager: SecurityManager
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
    private val requestCallbacks = SparseArray<(DeviceResponse?) -> Unit>()

    val isConnected: Boolean
        get() = connection.isConnected

    val deviceName: String
        get() = connection.deviceName

    fun disconnect() {
        launch {
            sendStopCommissioner()
            launch(UI) { connection.disconnect() }
        }
    }

    fun setConnectionPriority(priority: ConnectionPriority) {
        connection.setConnectionPriority(priority)
    }

    suspend fun sendGetConnectionStatus(): Result<GetConnectionStatusReply, ResultCode> {
        val response = sendRequest(GetConnectionStatusRequest.newBuilder().build())
        return buildResult(response) { r -> GetConnectionStatusReply.parseFrom(r.payloadData) }
    }

    suspend fun sendNetworkGetStatus(interfaceIndex: Int): Result<NetworkGetStatusReply, ResultCode> {
        val response = sendRequest(
                NetworkGetStatusRequest.newBuilder()
                        .setInterface(interfaceIndex)
                        .build()
        )
        return buildResult(response) { r -> NetworkGetStatusReply.parseFrom(r.payloadData) }
    }

    suspend fun sendNetworkGetConfig(interfaceIndex: Int): Result<NetworkGetConfigurationReply, ResultCode> {
        val response = sendRequest(
                NetworkGetConfigurationRequest.newBuilder()
                        .setInterface(interfaceIndex)
                        .build()
        )
        return buildResult(response) { r -> NetworkGetConfigurationReply.parseFrom(r.payloadData) }
    }

    suspend fun sendGetInterface(interfaceIndex: Int): Result<GetInterfaceReply, ResultCode> {
        val response = sendRequest(
                GetInterfaceRequest.newBuilder()
                        .setIndex(interfaceIndex)
                        .build()
        )
        return buildResult(response) { r -> GetInterfaceReply.parseFrom(r.payloadData) }
    }

    suspend fun sendGetInterfaceList(): Result<GetInterfaceListReply, ResultCode> {
        val response = sendRequest(GetInterfaceListRequest.newBuilder().build())
        return buildResult(response) { r -> GetInterfaceListReply.parseFrom(r.payloadData) }
    }

    suspend fun sendStartListeningMode(): Result<StartListeningModeReply, ResultCode> {
        val response = sendRequest(StartListeningModeRequest.newBuilder().build())
        return buildResult(response) { r -> StartListeningModeReply.parseFrom(r.payloadData) }
    }

    suspend fun sendStopListeningMode(): Result<StopListeningModeReply, ResultCode> {
        val response = sendRequest(StopListeningModeRequest.newBuilder().build())
        return buildResult(response) { r -> StopListeningModeReply.parseFrom(r.payloadData) }
    }

    suspend fun sendSetDeviceSetupDone(done: Boolean): Result<SetDeviceSetupDoneReply, ResultCode> {
        val response = sendRequest(
                SetDeviceSetupDoneRequest.newBuilder()
                        .setDone(done)
                        .build()
        )
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

    suspend fun sendCreateNetwork(
            name: String,
            password: String,
            channel: Int = DEFAULT_NETWORK_CHANNEL
    ): Result<CreateNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                CreateNetworkRequest.newBuilder()
                        .setName(name)
                        .setPassword(password)
                        .setChannel(channel)
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

    // NOTE: yes, 60 seconds is a CRAZY timeout, but... this is how long it takes to receive
    // a response sometimes.
    suspend fun sendJoinNetwork(): Result<JoinNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                JoinNetworkRequest.newBuilder().build()
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
        val requestFrame = message.asRequest()
        log.info { "Sending message ${message.javaClass} to '$connectionName': '$message' " +
             "with ID: ${requestFrame.requestId}" }


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
        synchronized(requestCallbacks) {
            requestCallbacks.remove(requestFrame.requestId.toInt())
        }
        return response
    }

    private fun doSendRequest(
            request: DeviceRequest,
            continuationCallback: (DeviceResponse?) -> Unit) {
        val requestCallback = { frame: DeviceResponse? -> continuationCallback(frame) }
        synchronized(requestCallbacks) {
            requestCallbacks.put(request.requestId.toInt(), requestCallback)
        }
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
