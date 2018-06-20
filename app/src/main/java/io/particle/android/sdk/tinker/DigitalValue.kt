package io.particle.android.sdk.tinker

enum class DigitalValue constructor(val intValue: Int) {

    HIGH(1),
    LOW(0),
    NONE(-1);

    companion object {

        @JvmStatic
        fun fromInt(value: Int): DigitalValue {
            return when (value) {
                1 -> HIGH
                0 -> LOW
                else -> NONE
            }
        }
    }

}
