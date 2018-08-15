package io.particle.particlemesh.meshsetup.connection.security

import io.particle.ecjpake4j.ECJPake
import io.particle.ecjpake4j.Role
import io.particle.particlemesh.common.toHex
import io.particle.particlemesh.meshsetup.connection.InboundFrameReader
import io.particle.particlemesh.meshsetup.connection.OutboundFrame
import io.particle.particlemesh.meshsetup.connection.OutboundFrameWriter
import kotlinx.coroutines.experimental.delay
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

    @Synchronized
    fun send(jpakeMessage: ByteArray) {
        frameWriter.writeFrame(OutboundFrame(jpakeMessage, jpakeMessage.size))
    }

    @Synchronized
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
        log.debug { "Sending round 1 to 'server', ${clientRoundOne?.size} bytes: ${clientRoundOne.toHex()}" }
        msgTransceiver.send(clientRoundOne!!)

        serverRoundOne = msgTransceiver.receive()
        log.debug { "Received ${serverRoundOne!!.size}-byte round 1 from 'server': ${serverRoundOne.toHex()}" }
        serverRoundTwo = msgTransceiver.receive()
        log.debug { "Received ${serverRoundTwo!!.size}-byte round 2 from 'server': ${serverRoundTwo.toHex()}" }

        log.debug { "Applying round 1 from 'server'" }
        jpakeImpl.receiveRemoteRoundOne(serverRoundOne!!)
        log.debug { "Applying round 2 from 'server'" }
        jpakeImpl.receiveRemoteRoundTwo(serverRoundTwo!!)

        clientRoundTwo = jpakeImpl.createLocalRoundTwo()
        log.debug { "Sending round 2 to 'server', ${clientRoundTwo?.size} bytes: ${clientRoundTwo.toHex()}" }
        msgTransceiver.send(clientRoundTwo!!)

        log.debug { "Calculating shared secret!" }
        return jpakeImpl.calculateSharedSecret()
    }

    private suspend fun confirmSharedSecret() {
//        log.warn { "Shared secret: ${sharedSecret.toHex()}" }

        val clientConfirmation = generateClientConfirmationData()
        log.debug { "Sending ${clientConfirmation.size}-byte confirmation message: ${clientConfirmation.toHex()}" }
        msgTransceiver.send(clientConfirmation)
        log.debug { "Awaiting confirmation response" }
        val serverConfirmation = msgTransceiver.receive()
        log.debug { "Confirmation response received with ${serverConfirmation.size} bytes: ${serverConfirmation.toHex()}" }
        val finalClientConfirmation = generateFinalConfirmation(clientConfirmation)

        if (Arrays.equals(serverConfirmation, finalClientConfirmation)) {
            log.debug { "Success!  Shared secret matches!" }
        } else {
            throw IOException("Cannot connect: local key confirmation data does not match remote!")
        }
    }

    private fun generateClientConfirmationData(): ByteArray {
        val hash = hashSha256(
                clientRoundOne!!,
                serverRoundOne!!,
                serverRoundTwo!!,
                clientRoundTwo!!
        )
        return hmacSha256(hash)
    }

    private fun generateFinalConfirmation(clientConfirmation: ByteArray): ByteArray {
        val confirmationData = hashSha256(
                clientRoundOne!!,
                serverRoundOne!!,
                serverRoundTwo!!,
                clientRoundTwo!!,
                clientConfirmation
        )
        return hmacSha256(confirmationData)
    }

    private fun hashSha256(vararg byteArrays: ByteArray): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        for (array in byteArrays) {
            digest.update(array)
        }
        return digest.digest()
    }

    private fun hmacSha256(includeInHmac: ByteArray): ByteArray {
        val signingKey = SecretKeySpec(sharedSecret!!, HMAC_SHA256)
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(signingKey)
        mac.update(Role.CLIENT.stringValue.toByteArray())
        mac.update(includeInHmac)
        return mac.doFinal()
    }
}


private const val HMAC_SHA256 = "HmacSHA256"
