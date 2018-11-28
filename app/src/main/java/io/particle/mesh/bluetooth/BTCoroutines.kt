package io.particle.mesh.bluetooth

import kotlinx.coroutines.newSingleThreadContext


/**
 * Single-threaded coroutine context to 100% ensure proper ordering for rx and tx of packets
 */
internal val packetTxRxContext = newSingleThreadContext("PACKET_TX_RX_CONTEXT")
