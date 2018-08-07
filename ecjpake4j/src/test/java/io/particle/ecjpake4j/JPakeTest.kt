package io.particle.ecjpake4j

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.util.*


private const val SHARED_SECRET = "tada, meat!"


class JPakeTest {

    private lateinit var jpakeClient: EcJPake
    private lateinit var jpakeServer: EcJPake

    @Test
    fun jpakeExchangeWorks() {
        jpakeClient = ECJPakeImpl(Role.CLIENT, SHARED_SECRET)
        jpakeServer = ECJPakeImpl(Role.SERVER, SHARED_SECRET)

        val (clientGenerated, serverGenerated) = performExchange()

        assertArrayEquals(clientGenerated, serverGenerated)
    }

    @Test
    fun jpakeExchangeFailsOnDifferentPassword() {
        jpakeClient = ECJPakeImpl(Role.CLIENT, SHARED_SECRET)
        jpakeServer = ECJPakeImpl(Role.SERVER, "bad password")

        val (clientGenerated, serverGenerated) = performExchange()

        val areEqual = Arrays.equals(clientGenerated, serverGenerated)
        assert(!areEqual) { "Server and client secrets should not match!" }
    }

    private fun performExchange(): Pair<ByteArray, ByteArray> {
        val c1 = jpakeClient.createLocalRoundOne()
        val s1 = jpakeServer.createLocalRoundOne()

        jpakeServer.receiveRemoteRoundOne(c1)
        val s2 = jpakeServer.createLocalRoundTwo()

        jpakeClient.receiveRemoteRoundOne(s1)
        jpakeClient.receiveRemoteRoundTwo(s2)

        val c2 = jpakeClient.createLocalRoundTwo()

        jpakeServer.receiveRemoteRoundTwo(c2)

        val clientGenerated = jpakeClient.calculateSharedSecret()
        val serverGenerated = jpakeServer.calculateSharedSecret()

        return Pair(clientGenerated, serverGenerated)
    }
}