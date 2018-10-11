package io.particle.mesh.setup


enum class WiFiStrength {

    STRONG,
    MEDIUM,
    WEAK;

    companion object {
        fun fromInt(value: Int): WiFiStrength {
            return when {
                (value >    0) -> throw IllegalArgumentException("Invalid RSSI value: $value")
                (value >  -56) -> STRONG
                (value >= -71) -> MEDIUM
                else -> WEAK
            }
        }
    }
}
