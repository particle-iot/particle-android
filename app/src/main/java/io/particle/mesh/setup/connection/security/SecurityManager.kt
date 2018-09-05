package io.particle.mesh.setup.connection.security

import io.particle.ecjpake4j.ECJPakeImpl
import io.particle.ecjpake4j.Role.CLIENT
import io.particle.mesh.setup.connection.InboundFrameReader
import io.particle.mesh.setup.connection.OutboundFrameWriter
import okio.Buffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


// Size of the AES key in bytes
private const val AES_CCM_KEY_SIZE = 16

// Size of the fixed part of the nonce
private const val AES_CCM_FIXED_NONCE_SIZE = 8

private const val ALGORITHM_TRANSFORMATION = "AES/CCM/NoPadding"


class SecurityManager {

    suspend fun createCryptoDelegate(
            jpakeLowEntropyPassword: String,
            frameWriter: OutboundFrameWriter,
            frameReader: InboundFrameReader
    ): AesCcmDelegate? {
        val transceiver = JpakeExchangeMessageTransceiver(frameWriter, frameReader)
        val jpakeManager = JpakeExchangeManager(
                ECJPakeImpl(CLIENT, jpakeLowEntropyPassword),
                transceiver
        )

        val jpakeSecret = jpakeManager.performJpakeExchange()

        return AesCcmDelegate.newDelegateFromJpakeSecret(jpakeSecret)
    }
}



// NOTE: this class just assumes that all encrypt operations will ues the request nonce,
// and all decrypt ops will use the reply nonce.  This is less flexible, but it's simpler
// than the alternatives
class AesCcmDelegate private constructor(
        private val aesKey: SecretKey,
        private val requestNonce: ByteArray,
        private val replyNonce: ByteArray
) {

    private var reqCount = 0
    private var respCount = 0

    companion object {
        fun newDelegateFromJpakeSecret(sharedSecret: ByteArray): AesCcmDelegate {
            val keySize = AES_CCM_KEY_SIZE
            val nonceSize = AES_CCM_FIXED_NONCE_SIZE

            val key = SecretKeySpec(sharedSecret, 0, keySize, "AES")
            // assuming a 32 byte secret, the request nonce comes from bytes 16-24 of the secret
            val reqNonce = sharedSecret.copyOfRange(keySize, keySize + nonceSize)
            // assuming a 32 byte secret, the reply nonce comes from bytes 24-32 of the secret
            val repNonce = sharedSecret.copyOfRange(keySize + nonceSize, keySize + nonceSize * 2)

            return AesCcmDelegate(key, reqNonce, repNonce)
        }
    }

    fun encrypt(bytes: ByteArray, additionalData: ByteArray): ByteArray {
        val id = ++reqCount
        val buf = Buffer()
        Nonces.writeRequestNonce(buf, id, requestNonce)
        val nonceVal = buf.readByteArray()

        // FIXME: look into reusing the instance but calling `init()` multiple times
        val encryptCipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION)
        encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(nonceVal))
        encryptCipher.updateAAD(additionalData)
        return encryptCipher.doFinal(bytes)
    }

    fun decrypt(bytes: ByteArray, additionalData: ByteArray): ByteArray {
        val id = ++respCount
        val buf = Buffer()
        Nonces.writeReplyNonce(buf, id, replyNonce)
        val nonceVal = buf.readByteArray()

        val cipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(nonceVal))
        cipher.updateAAD(additionalData)
        return cipher.doFinal(bytes)
    }
}
