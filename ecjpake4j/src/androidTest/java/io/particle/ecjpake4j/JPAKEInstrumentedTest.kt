package io.particle.ecjpake4j

import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


private const val SHARED_SECRET = "weak password"


/**
 * Instrumented test, which will execute on an Android device.
 */
@RunWith(AndroidJUnit4::class)
class JPAKEInstrumentedTest {

    private lateinit var jpakeClient: ECJPake
    private lateinit var jpakeServer: ECJPake

    @Test
    fun jpakeExchangeWorks() {
        jpakeClient = ECJPakeImpl(Role.CLIENT, SHARED_SECRET)
        jpakeServer = ECJPakeImpl(Role.SERVER, SHARED_SECRET)

        val (clientGenerated, serverGenerated) = performExchange()

        // JPAKE shared secret material should be 32 bytes
        Assert.assertEquals(32, clientGenerated.size)
        Assert.assertArrayEquals(clientGenerated, serverGenerated)
    }

    @Test
    fun jpakeExchangeFailsOnDifferentPassword() {
        jpakeClient = ECJPakeImpl(Role.CLIENT, SHARED_SECRET)
        jpakeServer = ECJPakeImpl(Role.SERVER, "LOL NO SECRET 4 U")

        val (clientGenerated, serverGenerated) = performExchange()

        val areEqual = Arrays.equals(clientGenerated, serverGenerated)
        assertFalse("Server and client secrets should not match!", areEqual)
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
