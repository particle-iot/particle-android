package io.particle.android.sdk.tinker


enum class PinAction {
    DIGITAL_READ,
    DIGITAL_WRITE,
    ANALOG_READ,
    ANALOG_WRITE,
    ANALOG_WRITE_DAC,
    NONE
}


enum class PinType {
    A,
    D
}


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
