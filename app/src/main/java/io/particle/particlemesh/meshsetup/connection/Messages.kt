package io.particle.particlemesh.meshsetup.connection


import android.support.annotation.MainThread
import android.util.SparseArray
import com.google.protobuf.AbstractMessage
import com.google.protobuf.ByteString
import com.google.protobuf.GeneratedMessageV3
import io.particle.firmwareprotos.ctrl.Common
import io.particle.firmwareprotos.ctrl.Common.ResultCode
import io.particle.firmwareprotos.ctrl.Config.*
import io.particle.firmwareprotos.ctrl.Extensions
import io.particle.firmwareprotos.ctrl.StorageOuterClass.*
import io.particle.firmwareprotos.ctrl.mesh.Mesh.*
import io.particle.particlemesh.bluetooth.PacketMTUSplitter
import io.particle.particlemesh.bluetooth.connecting.BTDeviceAddress
import io.particle.particlemesh.bluetooth.connecting.ConnectionPriority
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnection
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnectionFactory
import io.particle.particlemesh.bluetooth.packetTxRxContext
import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.common.Result
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.withTimeoutOrNull
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine


private var requestIdGenerator = AtomicInteger()


private fun generateRequestId(): Short {
    return requestIdGenerator.incrementAndGet().toShort()
}


internal fun AbstractMessage.asRequest(): RequestFrame {
    return RequestFrame(
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


class RequestSenderFactory(private val connectionFactory: MeshSetupConnectionFactory) {

    @MainThread
    suspend fun buildRequestSender(address: BTDeviceAddress, name: String): RequestSender? {


        val meshSetupConnection = connectionFactory.connectToDevice(address) ?: return null

        // 1. build up the "transmit" side
        val packetMTUSplitter = PacketMTUSplitter({ packet ->
            meshSetupConnection.packetSendChannel.offer(packet)
        })

        val frameWriter = FrameWriter { packetMTUSplitter.splitIntoPackets(it) }

        // 2. build the actual request sender
        val requestSender = RequestSender(frameWriter, meshSetupConnection, name)

        // 3. build up the "receive" side
        val frameReader = FrameReader { response -> requestSender.receiveResponse(response) }
        launch(packetTxRxContext) {
            for (packet in meshSetupConnection.packetReceiveChannel) {
                QATool.runSafely({ frameReader.receivePacket(packet) })
            }
        }

        return requestSender
    }

}


class RequestSender internal constructor(
        private val frameWriter: FrameWriter,
        private val connection: MeshSetupConnection,
        private val connectionName: String
) {

    private val log = KotlinLogging.logger {}
    private val requestCallbacks = SparseArray<(ResponseFrame?) -> Unit>()

    val isConnected: Boolean
        get() = connection.isConnected

    fun disconnect() {
        connection.disconnect()
    }

    fun setConnectionPriority(priority: ConnectionPriority) {
        connection.setConnectionPriority(priority)
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
        val response = sendRequest(JoinNetworkRequest.newBuilder().build())
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

    fun receiveResponse(responseFrame: ResponseFrame) {
        val callback = synchronized(requestCallbacks) {
            requestCallbacks.get(responseFrame.requestId.toInt())
        }

        if (callback != null) {
            callback(responseFrame)
        } else {
            // FIXME: handle the timeout case here better
            QATool.report(IllegalStateException("No callbacks found for request! ID: ${responseFrame.requestId}"))
            log.error { "No callbacks found for request! ID: ${responseFrame.requestId}" }
        }
    }

    //region PRIVATE
    private suspend fun sendRequest(message: GeneratedMessageV3): ResponseFrame? {
        log.info { "Sending message ${message.javaClass} to $connectionName: '$message'" }
        val requestFrame = message.asRequest()
        val response = withTimeoutOrNull(BLE_PROTO_REQUEST_TIMEOUT_MILLIS) {
            suspendCoroutine { continuation: Continuation<ResponseFrame?> ->
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
            requestCallbacks.remove(id)
        }
        return response
    }

    private fun doSendRequest(requestFrame: RequestFrame,
                              continuationCallback: (ResponseFrame?) -> Unit) {
        val requestCallback = { frame: ResponseFrame? -> continuationCallback(frame) }
        synchronized(requestCallbacks) {
            requestCallbacks.put(requestFrame.requestId.toInt(), requestCallback)
        }
        frameWriter.writeFrame(requestFrame)
    }

    private fun <V : GeneratedMessageV3> buildResult(
            response: ResponseFrame?,
            successTransformer: (ResponseFrame) -> V
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
