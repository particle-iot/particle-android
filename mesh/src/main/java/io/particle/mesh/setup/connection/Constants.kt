package io.particle.mesh.setup.connection

import java.util.*


val BT_SETUP_SERVICE_ID =           UUID.fromString("6FA90001-5C4E-48A8-94F4-8030546F36FC")
val BT_PROTOCOL_VERSION_ID =        UUID.fromString("6FA90002-5C4E-48A8-94F4-8030546F36FC")
// this is "TX" from the Device OS perspective, labelled as such to match docs elsewhere.
// from the mobile device perspective, this is the characteristic we will subscribe to
// (i.e.: read from)
val BT_SETUP_TX_CHARACTERISTIC_ID = UUID.fromString("6FA90003-5C4E-48A8-94F4-8030546F36FC")
// this is "RX" from the Device OS perspective
// from the mobile device perspective, this is the characteristic to write to for sending data
// to the device
val BT_SETUP_RX_CHARACTERISTIC_ID = UUID.fromString("6FA90004-5C4E-48A8-94F4-8030546F36FC")
