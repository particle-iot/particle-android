package io.particle.particlemesh.meshsetup.connection.security

import io.particle.particlemesh.common.toHex
import mu.KotlinLogging
import okio.Buffer
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


// Size of the AES key in bytes
const val AES_CCM_KEY_SIZE = 16

// Size of the fixed part of the nonce
const val AES_CCM_FIXED_NONCE_SIZE = 8


private const val ALGORITHM_TRANSFORMATION = "AES/CCM/NoPadding"


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

    private val log = KotlinLogging.logger {}

    @Synchronized
    fun encrypt(bytes: ByteArray, additionalData: ByteArray): ByteArray {
        // TODO: remove
        // relevant slice of Node CLI code:
//    const r = this._aesCcm.decrypt({
//        data: this._rxBuf.slice(MESSAGE_HEADER_SIZE, msgSize),
//        nonce: genReplyNonce(++this._repCount, this._repNonce),
//        tag: this._rxBuf.slice(msgSize, packetSize),
//        additionalData: this._rxBuf.slice(0, MESSAGE_HEADER_SIZE)

        val id = ++reqCount
        val buf = Buffer()
        Nonces.writeReplyNonce(buf, id, requestNonce)
        val nonceVal = buf.readByteArray()

        log.info { "Being asked to encrypt ${bytes.size} bytes: ${bytes.toHex()}" }

        val encryptCipher = Cipher.getInstance(ALGORITHM_TRANSFORMATION)
        encryptCipher.init(Cipher.ENCRYPT_MODE, aesKey, IvParameterSpec(nonceVal))
        encryptCipher.updateAAD(additionalData)

        val encrypted = encryptCipher.doFinal(bytes)
        log.info { "Final size of encrypted bytes: ${encrypted.size}" }
        return encrypted
    }

    @Synchronized
    fun decrypt(bytes: ByteArray): ByteArray {

        TODO("this is known not to work at the moment")

        val id = ++respCount
        val buf = Buffer()
        Nonces.writeReplyNonce(buf, id, requestNonce)
        val nonceVal = buf.readByteArray()

//        decryptCipher.updateAAD(nonceVal)
//        return decryptCipher.doFinal(bytes)
    }
}
