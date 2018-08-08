package io.particle.ecjpake4j

import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*


private const val SHARED_SECRET = "tada, meat!"


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(AndroidJUnit4::class)
class JPAKEInstrumentedTest {

    private lateinit var jpakeClient: EcJPake
    private lateinit var jpakeServer: EcJPake

    @Test
    fun jpakeExchangeWorks() {
        jpakeClient = ECJPakeImpl(Role.CLIENT, SHARED_SECRET)
        jpakeServer = ECJPakeImpl(Role.SERVER, SHARED_SECRET)

        val (clientGenerated, serverGenerated) = performExchange()

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
