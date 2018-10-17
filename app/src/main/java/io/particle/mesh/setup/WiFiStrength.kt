package io.particle.mesh.setup


enum class WiFiStrength(val sortValue: Int) {

    STRONG(3),
    MEDIUM(2),
    WEAK(1);

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
