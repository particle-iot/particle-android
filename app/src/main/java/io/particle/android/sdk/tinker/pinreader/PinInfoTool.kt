package io.particle.android.sdk.tinker.pinreader

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.tinker.pinreader.PinFunction.AnalogRead
import io.particle.android.sdk.tinker.pinreader.PinFunction.AnalogWriteDAC
import io.particle.android.sdk.tinker.pinreader.PinFunction.AnalogWritePWM
import io.particle.android.sdk.tinker.pinreader.PinFunction.DigitalRead
import io.particle.android.sdk.tinker.pinreader.PinFunction.DigitalWrite
import java.io.IOException


//region Result model types
data class ParticlePlatform(
    val deviceTypeId: Int,
    val deviceType: ParticleDeviceType,
    val pins: List<Pin>
)


enum class PinColumn {
    LEFT,
    RIGHT
}


data class Pin(
    val label: String,
    val tinkerName: String,
    val functions: List<PinFunction>,
    val column: PinColumn = PinColumn.LEFT
)


@JsonAdapter(PinFunctionAdapter::class)
sealed class PinFunction(val name: String) {

    object DigitalRead : PinFunction("DigitalRead")
    object DigitalWrite : PinFunction("DigitalWrite")
    object AnalogRead : PinFunction("AnalogRead")
    object AnalogWritePWM : PinFunction("AnalogWritePWM")
    object AnalogWriteDAC : PinFunction("AnalogWriteDAC")

    override fun toString(): String {
        return this.name
    }
}
//endregion


//region Converter functions

class PinFunctionAdapter : TypeAdapter<PinFunction?>() {

    companion object {
        private val nullWriter = Gson()
    }

    @Throws(IOException::class)
    override fun write(writer: JsonWriter, value: PinFunction?) {
        if (value == null) {
            synchronized(this) {
                nullWriter.toJson(value, writer)
            }
        } else {
            writer.value(value.name)
        }
    }

    @Throws(IOException::class)
    override fun read(reader: JsonReader): PinFunction? {
        return when (reader.nextString()) {
            DigitalRead.name -> DigitalRead
            DigitalWrite.name -> DigitalWrite
            AnalogRead.name -> AnalogRead
            AnalogWriteDAC.name -> AnalogWriteDAC
            AnalogWritePWM.name -> AnalogWritePWM
            else -> null
        }
    }
}
//endregion
