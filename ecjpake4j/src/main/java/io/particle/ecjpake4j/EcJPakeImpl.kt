package io.particle.ecjpake4j

import java.nio.ByteBuffer

class EcJPakeImpl(
        val role: Role,
        private val lowEntropySharedPassword: String
) : EcJPake {

    //    const unsigned char *secret, size_t len
    private lateinit var contextNativePointer: ByteBuffer
    

    fun prepareResources() {
        // TODO: call System.loadLibrary("mbedtls") here!
        System.loadLibrary("mbedtls")
    }

    override fun createLocalRoundOne(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveRemoteRoundOne(s1: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun receiveRemoteRoundTwo(s2: ByteArray) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createLocalRoundTwo(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateSharedSecret(): ByteArray {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /** Clear the state of the class and tell the underlying C lib to free the memory its using */
    fun releaseResources() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    // Finalizers aren't perfect, but they'll do here.
    fun finalize() {
        TODO("Tell C lib to release all pointers!")
    }

//    /** Return a ByteBuffer with a native pointer to the mbedtls_ecjpake_context */
    private external fun createContext(): ByteBuffer
    private external fun init()

}