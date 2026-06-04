package io.particle.mesh.setup

import io.particle.firmwareprotos.ctrl.mesh.Mesh
import io.particle.mesh.setup.connection.asRequest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test


class MessagesTest {

    @Test
    fun asRequestDoesTheThing() {
        val msg = Mesh.AuthRequest.newBuilder()
                .setPassword("LOLWUT")
                .build()
        val expectedPayload = msg.toByteArray()

        val request = msg.asRequest()

        assertEquals(1001.toShort(), request.messageType)
        assertArrayEquals(expectedPayload, request.payloadData)
        assertEquals(expectedPayload.size, request.payloadData.size)
    }

}
