package io.particle.ecjpake4j


interface EcJPake {

    /** Generate local round 1 ("c1")  */
    fun createLocalRoundOne(): ByteArray

    /** Read round one from other side ("s1")  */
    fun receiveRemoteRoundOne(s1: ByteArray)

    /** Read round two from other side ("s2")  */
    fun receiveRemoteRoundTwo(s2: ByteArray)

    /** Create local round 2 ("c2")  */
    fun createLocalRoundTwo(): ByteArray

    /** Calculate the shared secret, from which the key will be derived */
    fun calculateSharedSecret(): ByteArray

}


enum class Role(val stringValue: String, val intValue: Int) {
    CLIENT("client", 0),
    SERVER("server", 1)
}
