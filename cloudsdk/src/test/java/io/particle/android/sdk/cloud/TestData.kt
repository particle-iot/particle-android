package io.particle.android.sdk.cloud

import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType

const val DEVICE_ID_0 = "d34db33f52ca40bd34db33f0"
const val DEVICE_ID_1 = "d34db33f52ca40bd34db33f1"

internal val DEVICE_STATE_0 = DeviceState(
    DEVICE_ID_0,
    ParticleDeviceType.XENON.intValue,
    ParticleDeviceType.XENON.intValue,
    "64.124.183.01",
    null,
    "normal",
    "device0",
    true,
    false,
    null,
    null,
    "1.4.0",
    "1.4.4",
    setOf(),
    mapOf(),
    ParticleDeviceType.XENON,
    null,
    "XENKAB8D34DB33F",
    "ABCDEFG01234567",
    null,
    "1.4.0",
    null
)

internal val DEVICE_STATE_1 = DeviceState(
    DEVICE_ID_1,
    ParticleDeviceType.ARGON.intValue,
    ParticleDeviceType.ARGON.intValue,
    "64.124.183.02",
    null,
    "normal",
    "device1",
    true,
    false,
    null,
    null,
    "1.4.2",
    "1.4.4",
    setOf(),
    mapOf(),
    ParticleDeviceType.ARGON,
    null,
    "ARGHAB8D34DB33F",
    "ABCDEFG01234567",
    null,
    "1.4.2",
    null
)

val DEVICE_0_JSON = """
{
    "id":"$DEVICE_ID_0",
    "name":"device0",
    "last_app":null,
    "last_ip_address":"64.124.183.01",
    "last_heard":null,
    "product_id":14,
    "connected":true,
    "platform_id":14,
    "cellular":false,
    "notes":null,
    "status":"normal",
    "serial_number":"XENKAB8D34DB33F",
    "mobile_secret":"ABCDEFG01234567",
    "current_build_target":"1.4.0",
    "system_firmware_version":"1.4.0",
    "default_build_target":"1.4.4"
}
""".trimIndent()


val DEVICE_1_JSON = """
{
    "id":"$DEVICE_ID_1",
    "name":"device1",
    "last_app":null,
    "last_ip_address":"64.124.183.02",
    "last_heard":null,
    "product_id":12,
    "connected":true,
    "platform_id":12,
    "cellular":false,
    "notes":null,
    "status":"normal",
    "serial_number":"ARGHAB8D34DB33F",
    "mobile_secret":"ABCDEFG01234567",
    "current_build_target":"1.4.2",
    "system_firmware_version":"1.4.2",
    "default_build_target":"1.4.4"
}
""".trimIndent()


val DEVICE_LIST_JSON = """
[   
    $DEVICE_0_JSON,
    $DEVICE_1_JSON
]
""".trimIndent()
