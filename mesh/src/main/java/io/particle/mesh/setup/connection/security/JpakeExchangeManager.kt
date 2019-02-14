package io.particle.mesh.setup.connection.security

import io.particle.ecjpake4j.ECJPake
import io.particle.ecjpake4j.Role
import io.particle.mesh.setup.connection.InboundFrameReader
import io.particle.mesh.setup.connection.OutboundFrame
import io.particle.mesh.setup.connection.OutboundFrameWriter
import mu.KotlinLogging
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


class JpakeExchangeMessageTransceiver(
        private val frameWriter: OutboundFrameWriter,
        private val frameReader: InboundFrameReader
) {

    fun send(jpakeMessage: ByteArray) {
        frameWriter.writeFrame(OutboundFrame(jpakeMessage, jpakeMessage.size))
    }

    suspend fun receive(): ByteArray {
        val jpakeFrame = frameReader.inboundFrameChannel.receive()
        return jpakeFrame.frameData
    }

}



// NOTE: this class assumes it's in the *client* role.
// Some of the details below would have to change to support the server role
// e.g.: order of message exchange
class JpakeExchangeManager(
        private val jpakeImpl: ECJPake,
        private val msgTransceiver: JpakeExchangeMessageTransceiver
) {

    private var clientRoundOne: ByteArray? = null
    private var clientRoundTwo: ByteArray? = null
    private var serverRoundOne: ByteArray? = null
    private var serverRoundTwo: ByteArray? = null
    private var sharedSecret: ByteArray? = null

    private val log = KotlinLogging.logger {}

    /** Perform the JPAKE exchange (including confirmation) and return the shared secret. */
    @Throws(IOException::class)
    suspend fun performJpakeExchange(): ByteArray {
        log.debug { "performJpakeExchange()" }
        try {
            sharedSecret = doPerformExchange()
            confirmSharedSecret()
            return sharedSecret!!

        } finally {
            clientRoundOne = null
            clientRoundTwo = null
            serverRoundOne = null
            serverRoundTwo = null
            sharedSecret = null
        }
    }

    private suspend fun doPerformExchange(): ByteArray {
        clientRoundOne = jpakeImpl.createLocalRoundOne()
        log.debug { "Sending round 1 to 'server', ${clientRoundOne?.size} bytes" }
        msgTransceiver.send(clientRoundOne!!)

        serverRoundOne = msgTransceiver.receive()
        log.debug { "Received ${serverRoundOne!!.size}-byte round 1 from 'server'" }
        serverRoundTwo = msgTransceiver.receive()
        log.debug { "Received ${serverRoundTwo!!.size}-byte round 2 from 'server'" }

        log.debug { "Applying round 1 from 'server'" }
        jpakeImpl.receiveRemoteRoundOne(serverRoundOne!!)
        log.debug { "Applying round 2 from 'server'" }
        jpakeImpl.receiveRemoteRoundTwo(serverRoundTwo!!)

        clientRoundTwo = jpakeImpl.createLocalRoundTwo()
        log.debug { "Sending round 2 to 'server', ${clientRoundTwo?.size}" }
        msgTransceiver.send(clientRoundTwo!!)

        log.debug { "Calculating shared secret!" }
        return jpakeImpl.calculateSharedSecret()
    }

    private suspend fun confirmSharedSecret() {
        log.debug { "Shared secret: ${sharedSecret!!.size} bytes" }

        val clientConfirmation = generateClientConfirmationData()
        log.debug { "Sending ${clientConfirmation.size}-byte confirmation message" }
        msgTransceiver.send(clientConfirmation)
        log.debug { "Awaiting confirmation response" }
        val serverConfirmation = msgTransceiver.receive()
        log.debug { "Confirmation response received with ${serverConfirmation.size}" }
        val finalClientConfirmation = generateFinalConfirmation(clientConfirmation)

        if (Arrays.equals(serverConfirmation, finalClientConfirmation)) {
            log.debug { "Success!  Shared secret matches!" }
        } else {
            throw IOException("Cannot connect: local key confirmation data does not match remote!")
        }
    }

    private fun generateClientConfirmationData(): ByteArray {
        val roundsHash = shaHash(
                clientRoundOne!!,
                serverRoundOne!!,
                serverRoundTwo!!,
                clientRoundTwo!!
        )

        return hmacWithJpakeTagInKey(roundsHash, Role.CLIENT, Role.SERVER)
    }

    private fun generateFinalConfirmation(clientConfirmation: ByteArray): ByteArray {
        val confirmationData = shaHash(
                clientRoundOne!!,
                serverRoundOne!!,
                serverRoundTwo!!,
                clientRoundTwo!!,
                clientConfirmation
        )
        return hmacWithJpakeTagInKey(confirmationData, Role.SERVER, Role.CLIENT)
    }

    private fun shaHash(vararg byteArrays: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        for (array in byteArrays) {
            digest.update(array)
        }
        return digest.digest()
    }

    private fun hmacWithJpakeTagInKey(includeInHmac: ByteArray, role1: Role, role2: Role): ByteArray {
        val macKeyMaterial = shaHash(sharedSecret!!, "JPAKE_KC".toByteArray())
        val macKey = SecretKeySpec(macKeyMaterial, HMAC_SHA256)
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(macKey)
        mac.update("KC_1_U")
        mac.update(role1.stringValue)
        mac.update(role2.stringValue)
        mac.update(includeInHmac)
        return mac.doFinal()
    }
}

private fun Mac.update(str: String) {
    this.update(str.toByteArray())
}

private const val HMAC_SHA256 = "HmacSHA256"
