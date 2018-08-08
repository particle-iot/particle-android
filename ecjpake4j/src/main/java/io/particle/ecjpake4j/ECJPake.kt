package io.particle.ecjpake4j


interface ECJPake {

    /**
     * Generate local round 1 ("c1")
     *
     * @throws [IllegalStateException] if an error occurs at this step inside mbedtls
     */
    @Throws(IllegalStateException::class)
    fun createLocalRoundOne(): ByteArray

    /**
     * Read round one from other side ("s1")
     *
     * @throws [IllegalStateException] if an error occurs at this step inside mbedtls
     */
    @Throws(IllegalStateException::class)
    fun receiveRemoteRoundOne(s1: ByteArray)

    /**
     * Read round two from other side ("s2")
     *
     * @throws [IllegalStateException] if an error occurs at this step inside mbedtls
     */
    @Throws(IllegalStateException::class)
    fun receiveRemoteRoundTwo(s2: ByteArray)

    /**
     * Create local round 2 ("c2")
     *
     * @throws [IllegalStateException] if an error occurs at this step inside mbedtls
     */
    @Throws(IllegalStateException::class)
    fun createLocalRoundTwo(): ByteArray

    /**
     * Calculate the shared secret, from which the key will be derived
     *
     * @throws [IllegalStateException] if an error occurs at this step inside mbedtls
     */
    @Throws(IllegalStateException::class)
    fun calculateSharedSecret(): ByteArray

}


enum class Role(val stringValue: String, val intValue: Int) {
    CLIENT("client", 0),
    SERVER("server", 1)
}
