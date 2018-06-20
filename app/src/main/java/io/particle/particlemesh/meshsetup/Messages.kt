package io.particle.particlemesh.meshsetup


import android.support.annotation.MainThread
import android.util.SparseArray
import com.google.protobuf.AbstractMessage
import io.particle.particlemesh.bluetooth.PacketMTUSplitter
import io.particle.particlemesh.bluetooth.connecting.BTDeviceAddress
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnection
import io.particle.particlemesh.bluetooth.connecting.MeshSetupConnectionFactory
import io.particle.particlemesh.bluetooth.packetTxRxContext
import io.particle.particlemesh.common.QATool
import io.particle.particlemesh.common.Result
import io.particle.firmwareprotos.ctrl.Common
import io.particle.firmwareprotos.ctrl.Extensions
import io.particle.firmwareprotos.ctrl.mesh.Mesh
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
private fun Short.toResultCode(): Common.ResultCode {
    val asInt = this.toInt()
    return when (asInt) {
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
    suspend fun buildRequestSender(address: BTDeviceAddress): RequestSender? {


        val meshSetupConnection = connectionFactory.connectToDevice(address) ?: return null

        // 1. build up the "transmit" side
        val packetMTUSplitter = PacketMTUSplitter({ packet ->
            meshSetupConnection.packetSendChannel.offer(packet)
        })
//        val loopbackChannel =
//        val packetMTUSplitter = PacketMTUSplitter({ packet ->
//            meshSetupConnection.packetSendChannel.offer(packet)
//        })
        val frameWriter = FrameWriter({ packetMTUSplitter.splitIntoPackets(it) })

        // 2. build the actual request sender
        val requestSender = RequestSender(frameWriter, meshSetupConnection)

        // 3. build up the "receive" side
        val frameReader = FrameReader({ response -> requestSender.receiveResponse(response) })
        launch(packetTxRxContext) {
            for (packet in meshSetupConnection.packetReceiveChannel) {
                QATool.runSafely({ frameReader.receivePacket(packet) })
            }
        }

        return requestSender
    }

}


interface AbstractRequestSender {

    suspend fun sendGetNetworkInfo(): Result<Mesh.GetNetworkInfoReply, Common.ResultCode>

    suspend fun sendCreateNetwork(name: String,
                                  password: String
    ): Result<Mesh.CreateNetworkReply, Common.ResultCode>

    suspend fun sendAuth(commissionerCredential: String): Result<Mesh.AuthReply, Common.ResultCode>

    suspend fun sendStartCommissioner(): Result<Mesh.StartCommissionerReply, Common.ResultCode>

    suspend fun sendPrepareJoiner(network: Mesh.NetworkInfo
    ): Result<Mesh.PrepareJoinerReply, Common.ResultCode>

    suspend fun sendAddJoiner(eui64: String,
                              joiningCredential: String
    ): Result<Mesh.AddJoinerReply, Common.ResultCode>

    suspend fun sendJoinNetwork(): Result<Mesh.JoinNetworkReply, Common.ResultCode>

    suspend fun sendStopCommissioner(): Result<Mesh.StopCommissionerReply, Common.ResultCode>

    suspend fun sendLeaveNetwork(): Result<Mesh.LeaveNetworkReply, Common.ResultCode>

}


class RequestSender internal constructor(
        private val frameWriter: FrameWriter,
        private val connection: MeshSetupConnection
) : AbstractRequestSender {

    private val log = KotlinLogging.logger {}
    private val requestCallbacks = SparseArray<(ResponseFrame?) -> Unit>()

    val isConnected: Boolean
        get() = connection.isConnected

    fun disconnect() {
        connection.disconnect()
    }

    override suspend fun sendGetNetworkInfo(): Result<Mesh.GetNetworkInfoReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.GetNetworkInfoRequest.newBuilder()
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.GetNetworkInfoReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendCreateNetwork(name: String, password: String
    ): Result<Mesh.CreateNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.CreateNetworkRequest.newBuilder()
                        .setName(name)
                        .setPassword(password)
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.CreateNetworkReply.parseFrom(r.payloadData) }
        )
    }

    override suspend fun sendAuth(commissionerCredential: String): Result<Mesh.AuthReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.AuthRequest.newBuilder()
                        .setPassword(commissionerCredential)
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.AuthReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendStartCommissioner(): Result<Mesh.StartCommissionerReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.StartCommissionerRequest.newBuilder()
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.StartCommissionerReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendPrepareJoiner(network: Mesh.NetworkInfo): Result<Mesh.PrepareJoinerReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.PrepareJoinerRequest.newBuilder()
                        .setNetwork(network)
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.PrepareJoinerReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendAddJoiner(eui64: String, joiningCredential: String): Result<Mesh.AddJoinerReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.AddJoinerRequest.newBuilder()
                        .setEui64(eui64)
                        .setPassword(joiningCredential)
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.AddJoinerReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendJoinNetwork(): Result<Mesh.JoinNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.JoinNetworkRequest.newBuilder()
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.JoinNetworkReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendStopCommissioner(): Result<Mesh.StopCommissionerReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.StopCommissionerRequest.newBuilder()
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.StopCommissionerReply.parseFrom(r.payloadData) })
    }

    override suspend fun sendLeaveNetwork(): Result<Mesh.LeaveNetworkReply, Common.ResultCode> {
        val response = sendRequest(
                Mesh.LeaveNetworkRequest.newBuilder()
                        .build()
                        .asRequest()
        )
        return buildResult(response, { r -> Mesh.LeaveNetworkReply.parseFrom(r.payloadData) })
    }

    suspend fun sendEchoRequest(bytes: ByteArray): Result<ByteArray, Common.ResultCode> {
        val response = sendRequest(RequestFrame(generateRequestId(), 1111, bytes))
        return buildResult(response, { r -> r.payloadData })
    }

    fun receiveResponse(responseFrame: ResponseFrame) {
        val callback = requestCallbacks.get(responseFrame.requestId.toInt())
        if (callback != null) {
            callback(responseFrame)
        } else {
            QATool.illegalState("No callbacks found for request! ID: ${responseFrame.requestId}")
        }
    }

    private suspend fun sendRequest(requestFrame: RequestFrame): ResponseFrame? {
        val response = withTimeoutOrNull(BLE_PROTO_REQUEST_TIMEOUT_MILLIS) {
            suspendCoroutine { continuation: Continuation<ResponseFrame?> ->
                doSendRequest(requestFrame) { continuation.resume(it) }
            }
        }
        // by the time we get to here, our callback has been used up,
        // so we can remove it from the map
        synchronized(requestCallbacks) {
            requestCallbacks.remove(requestFrame.requestId.toInt())
        }
        return response
    }

    private fun doSendRequest(requestFrame: RequestFrame,
                              continuationCallback: (ResponseFrame?) -> Unit) {
        log.info { "Sending request $requestFrame" }
        val requestCallback = { frame: ResponseFrame? -> continuationCallback(frame) }
        synchronized(requestCallbacks) {
            requestCallbacks.put(requestFrame.requestId.toInt(), requestCallback)
        }
        frameWriter.writeFrame(requestFrame)
    }

    private fun <V> buildResult(
            response: ResponseFrame?,
            successTransformer: (ResponseFrame) -> V
    ): Result<V, Common.ResultCode> {
        if (response == null) {
            return Result.Absent()
        }

        return if (response.resultCode == 0.toShort()) {
            Result.Present(successTransformer(response))
        } else {
            Result.Error(response.resultCode.toResultCode())
        }
    }

}
