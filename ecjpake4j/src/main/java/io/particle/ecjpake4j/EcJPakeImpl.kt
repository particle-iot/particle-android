package io.particle.ecjpake4j

class EcJPakeImpl(
        val role: Role,
        private val lowEntropySharedPassword: String
) : EcJPake {

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

}