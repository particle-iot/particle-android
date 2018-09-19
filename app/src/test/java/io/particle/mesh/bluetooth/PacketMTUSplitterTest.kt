package io.particle.mesh.bluetooth

import org.junit.Assert.assertEquals
import org.junit.Test


class PacketMTUSplitterTest {

    @Test
    fun doTheThing() {
        val hexBytes = "word1word2word3word4word5".toByteArray()
        val outputTarget = mutableListOf<ByteArray>()
        val testee = PacketMTUSplitter( { outputTarget.add(it) } , mtuSize = 10)

        testee.splitIntoPackets(hexBytes)

        assertEquals(3, outputTarget.size)
    }

}