package io.particle.mesh.setup

import io.particle.mesh.setup.connection.InboundFrameReader
import io.particle.mesh.setup.utils.readByteArray
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder


class FramingTest {

//    @Test
//    fun frameReaderReadsFrames() {
//        val td = FrameTestData()
//        var callbackCount = 0
//        val testSubject = InboundFrameReader({ frame ->
//            assertEquals(td.expectedRequestId, frame.requestId)
//            assertEquals(td.expectedResultCode, frame.resultCode)
//            assertEquals(td.expectedPayload.size, frame.payloadData.size)
//            assertEquals(td.expectedRequestId, frame.requestId)
//            callbackCount++
//        })
//
//        // by calling receivePacket(), the asserts will be run in the callback
//        testSubject.receivePacket(td.responseFrameData)
//        assertEquals(1, callbackCount)
//    }
//
//    @Test
//    fun handleFrameAcrossMultiplePackets() {
//        val td = FrameTestData()
//        var callbackCount = 0
//        val testSubject = InboundFrameReader({ frame ->
//            assertEquals(td.expectedRequestId, frame.requestId)
//            assertEquals(td.expectedResultCode, frame.resultCode)
//            assertEquals(td.expectedPayload.size, frame.payloadData.size)
//            assertEquals(td.expectedRequestId, frame.requestId)
//            callbackCount++
//        })
//
//        val buffer = ByteBuffer.wrap(td.responseFrameData)
//
//        // send all of the header data + a few of the data bytes in the first packet
//        testSubject.receivePacket(buffer.readByteArray(15))
//        assertEquals(0, callbackCount)
//        // send the rest of the data
//        testSubject.receivePacket(buffer.readByteArray())
//        assertEquals(1, callbackCount)
//    }

}


private class FrameTestData {

    val expectedRequestId = 123.toShort()
    val expectedResultCode = (-270).toShort()
    val expectedPayloadText = "AintNoPartyLikeATestingParty"
    val expectedPayload = expectedPayloadText.toByteArray()
    val responseFrameData: ByteArray

    init {
        val buffer = ByteBuffer.allocate(1024)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(expectedRequestId)
                .putShort(expectedResultCode)
                .putInt(expectedPayload.size)
                .put(expectedPayload)
        buffer.flip()
        responseFrameData = buffer.readByteArray()
    }
}
