package io.particle.mesh.setup.connection.security

import io.particle.ecjpake4j.ECJPakeImpl
import io.particle.ecjpake4j.Role
import io.particle.mesh.setup.connection.InboundFrameReader
import io.particle.mesh.setup.connection.OutboundFrameWriter


class CryptoDelegateFactory {

    suspend fun createCryptoDelegate(
            frameWriter: OutboundFrameWriter,
            frameReader: InboundFrameReader,
            jpakeLowEntropyPassword: String
    ): AesCcmDelegate? {

        val transceiver = JpakeExchangeMessageTransceiver(frameWriter, frameReader)
        val jpakeManager = JpakeExchangeManager(
                ECJPakeImpl(Role.CLIENT, jpakeLowEntropyPassword),
                transceiver
        )

        val jpakeSecret = jpakeManager.performJpakeExchange()

        return AesCcmDelegate.newDelegateFromJpakeSecret(jpakeSecret)
    }

}