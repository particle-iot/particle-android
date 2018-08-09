package io.particle.particlemesh.meshsetup.connection.security

import io.particle.ecjpake4j.ECJPake
import io.particle.ecjpake4j.Role
import java.io.IOException
import java.security.MessageDigest
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec



class MessageTransceiver {

    fun send(clientConfirm: ByteArray) {
        TODO("not implemented")
    }

    suspend fun receive(): ByteArray {
        TODO("not implemented")
    }

}


// NOTE: this class assumes it's in the *client* role.
// Some of the details below would have to change to support the server role
// e.g.: order of message exchange
class JpakeExchangeManager(
        private val jpakeImpl: ECJPake,
        private val msgTransceiver: MessageTransceiver
) {

    private var clientRoundOne: ByteArray? = null
    private var clientRoundTwo: ByteArray? = null
    private var serverRoundOne: ByteArray? = null
    private var serverRoundTwo: ByteArray? = null
    private var sharedSecret: ByteArray? = null

    /** Perform the JPAKE exchange (including confirmation) and return the shared secret. */
    @Throws(IOException::class)
    suspend fun performJpakeExchange(): ByteArray {
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
        msgTransceiver.send(clientRoundOne!!)

        serverRoundOne = msgTransceiver.receive()
        serverRoundTwo = msgTransceiver.receive()
        jpakeImpl.receiveRemoteRoundOne(serverRoundOne!!)
        jpakeImpl.receiveRemoteRoundTwo(serverRoundTwo!!)

        clientRoundTwo = jpakeImpl.createLocalRoundTwo()
        msgTransceiver.send(clientRoundTwo!!)

        return jpakeImpl.calculateSharedSecret()
    }

    private suspend fun confirmSharedSecret() {
        val clientConfirmation = generateClientConfirmationData()
        msgTransceiver.send(clientConfirmation)
        val serverConfirmation = msgTransceiver.receive()
        val finalClientConfirmation = generateFinalConfirmation(clientConfirmation)

        if (!Arrays.equals(serverConfirmation, finalClientConfirmation)) {
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


private const val HMAC_SHA256 = "HmacSHA1"