package io.particle.ecjpake4j

import java.nio.ByteBuffer


typealias CPointer = ByteBuffer

internal const val RNG_SEED_DATA = "android-mbedtls"


class ECJPakeImpl(
        val role: Role,
        private val lowEntropySharedPassword: String
) : ECJPake {

    private lateinit var cryptoComponents: CPointer

    init {
        prepareResources()
    }

    private fun prepareResources() {
        System.loadLibrary("ecjpake-jni-wrapper")

        // NOTE: some of these functions depend on the ordering below. Don't change the ordering
        // of these calls unless you understand exactly what each of these functions does
        // in the C layer.
        cryptoComponents = createNativeComponents(RNG_SEED_DATA)
        setupJpake(cryptoComponents, role.intValue, lowEntropySharedPassword)
    }

    override fun createLocalRoundOne(): ByteArray {
        val result = writeRoundOne(cryptoComponents)
        if (result != null) {
            return result.copyOf()
        } else {
            throw IllegalStateException("Error occurred in mbedtls lib for createLocalRoundOne()")
        }
    }

    override fun receiveRemoteRoundOne(s1: ByteArray) {
        val result = readRoundOne(cryptoComponents, s1.copyOf())
        if (result != 0) {
            throw IllegalStateException("Error $result occurred in mbedtls lib for receiveRemoteRoundOne()")
        }
    }

    override fun receiveRemoteRoundTwo(s2: ByteArray) {
        val result = readRoundTwo(cryptoComponents, s2.copyOf())
        if (result != 0) {
            throw IllegalStateException("Error occurred in mbedtls lib for receiveRemoteRoundTwo()")
        }
    }

    override fun createLocalRoundTwo(): ByteArray {
        val result = writeRoundTwo(cryptoComponents)
        if (result != null) {
            return result.copyOf()
        } else {
            throw IllegalStateException("Error occurred in mbedtls lib for createLocalRoundTwo()")
        }
    }

    override fun calculateSharedSecret(): ByteArray {
        val result = deriveSecret(cryptoComponents)
        if (result != null) {
            return result.copyOf()
        } else {
            throw IllegalStateException("Error occurred in mbedtls lib for calculateSharedSecret()")
        }
    }

    // Finalizers aren't perfect, but they'll do here.
    fun finalize() {
        freePointers(cryptoComponents)
    }

    private external fun createNativeComponents(rngSeedData: String): CPointer

    /**
     * Call mbedtls_ecjpake_setup.  (The hash and curve args are statically defined in the C code)
     *
     * @returns 0 if successful, otherwise a negative error code.
     */
    private external fun setupJpake(ctxPointer: CPointer, role: Int, secret: String): Int

    /**
     * Call mbedtls_ecjpake_write_round_one and return the generated local round one message.
     *
     * @returns null if not successful
     */
    private external fun writeRoundOne(cryptoComponents: CPointer): ByteArray?

    /**
     * Call	mbedtls_ecjpake_read_round_one with the remote's round one message data.
     *
     * @returns 0 if successful, otherwise a negative error code.
     */
    private external fun readRoundOne(cryptoComponents: CPointer, remoteRoundOneMessageData: ByteArray): Int

    /**
     * Call mbedtls_ecjpake_write_round_two and return the generated local round two message.
     *
     * @returns null if not successful
     */
    private external fun writeRoundTwo(cryptoComponents: CPointer): ByteArray?

    /**
     * Call	mbedtls_ecjpake_read_round_two with the remote's round two message data.
     *
     * @returns 0 if successful, otherwise a negative error code.
     */
    private external fun readRoundTwo(cryptoComponents: CPointer, remoteRoundTwoMessageData: ByteArray): Int

    /**
     * Call mbedtls_ecjpake_derive_secret to derive the shared secret
     *
     * @returns null if not successful
     */
    private external fun deriveSecret(cryptoComponents: CPointer): ByteArray?

    /**
     * Free all pointers using their respective functions
     */
    private external fun freePointers(cryptoComponents: CPointer)

}