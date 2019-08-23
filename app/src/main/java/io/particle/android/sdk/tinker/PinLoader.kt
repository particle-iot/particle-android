package io.particle.android.sdk.tinker

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.tinker.PinType.A
import io.particle.android.sdk.tinker.PinType.D
import io.particle.android.sdk.tinker.pinreader.ParticlePlatform
import io.particle.android.sdk.tinker.pinreader.PinColumn.LEFT
import io.particle.android.sdk.tinker.pinreader.PinColumn.RIGHT
import io.particle.android.sdk.tinker.pinreader.PinFunction
import io.particle.android.sdk.tinker.pinreader.PinFunction.AnalogRead
import io.particle.android.sdk.tinker.pinreader.PinFunction.AnalogWriteDAC
import io.particle.android.sdk.tinker.pinreader.PinFunction.AnalogWritePWM
import io.particle.android.sdk.tinker.pinreader.PinFunction.DigitalRead
import io.particle.android.sdk.tinker.pinreader.PinFunction.DigitalWrite
import io.particle.android.sdk.utils.readRawResourceBytes
import io.particle.android.sdk.utils.ui.Ui
import io.particle.sdk.app.R
import mu.KotlinLogging
import java.util.*
import kotlin.math.min


private val LEFT_PIN_VIEWS = listOf(
    R.id.tinker_pin_left1,
    R.id.tinker_pin_left2,
    R.id.tinker_pin_left3,
    R.id.tinker_pin_left4,
    R.id.tinker_pin_left5,
    R.id.tinker_pin_left6,
    R.id.tinker_pin_left7,
    R.id.tinker_pin_left8,
    R.id.tinker_pin_left9,
    R.id.tinker_pin_left10,
    R.id.tinker_pin_left11
)

private val RIGHT_PIN_VIEWS = listOf(
    R.id.tinker_pin_right1,
    R.id.tinker_pin_right2,
    R.id.tinker_pin_right3,
    R.id.tinker_pin_right4,
    R.id.tinker_pin_right5,
    R.id.tinker_pin_right6,
    R.id.tinker_pin_right7,
    R.id.tinker_pin_right8,
    R.id.tinker_pin_right9,
    R.id.tinker_pin_right10,
    R.id.tinker_pin_right11
)


private fun PinFunction.toPinAction(): PinAction {
    return when (this) {
        DigitalRead -> PinAction.DIGITAL_READ
        DigitalWrite -> PinAction.DIGITAL_WRITE
        AnalogRead -> PinAction.ANALOG_READ
        AnalogWritePWM -> PinAction.ANALOG_WRITE
        AnalogWriteDAC -> PinAction.ANALOG_WRITE_DAC
    }
}


internal fun loadPins(deviceType: ParticleDeviceType, frag: Fragment): List<Pin> {

    val platforms = loadFromJson(frag.requireContext())
    val default = platforms[ParticleDeviceType.ARGON] ?: error("Photon pin data not found!")
    val platform = platforms.getOrElse(deviceType) { default }

    val leftPinViews = LinkedList(LEFT_PIN_VIEWS.reversed())
    val rightPinViews = LinkedList(RIGHT_PIN_VIEWS)

    val leftPins = platform.pins.filter { it.column == LEFT }.reversed()
    val rightPins = platform.pins.filter { it.column == RIGHT }.reversed()

    val result: MutableList<Pin> = mutableListOf()
    for ((pins, views) in listOf(Pair(leftPins, leftPinViews), Pair(rightPins, rightPinViews))) {
        for (pin in pins) {
            if (views.isEmpty()) {
                log.warn { "Out of free pin views in ${pin.column} column for this $deviceType" }
                break
            }
            val viewId = views.pop()
            val type = if (pin.column == LEFT) A else D
            val functions = pin.functions.map { it.toPinAction() }.toSet()
            result.add(
                Pin(frag.findPinView(viewId), type, pin.tinkerName, functions, pin.label)
            )
        }
    }

    val numPinsToMakeGone = min(
        LEFT_PIN_VIEWS.size - leftPins.size,
        RIGHT_PIN_VIEWS.size - rightPins.size
    )

    for (viewIdList in listOf(leftPinViews, rightPinViews)) {
        for (i in 1..numPinsToMakeGone) {
            if (viewIdList.isEmpty()) {
                break
            }
            val viewId = viewIdList.pop()
            val view = frag.findPinView(viewId)
            (view.parent as ViewGroup).visibility = View.GONE
        }
    }

    for (pinList in listOf(leftPinViews, rightPinViews)) {
        for (pinId in pinList) {
            val view = frag.findPinView(pinId)
            (view.parent as ViewGroup).visibility = View.INVISIBLE
        }
    }

    return result
}

private val log = KotlinLogging.logger {}

private fun loadFromJson(context: Context): Map<ParticleDeviceType, ParticlePlatform> {
    val jsonString = context.readRawResourceBytes(R.raw.tinker_pin_data).toString(Charsets.UTF_8)
    val listType = object : TypeToken<ArrayList<ParticlePlatform>>() {}.type
    val platformList = Gson().fromJson<ArrayList<ParticlePlatform>>(
        jsonString,
        listType
    )
    return platformList.map { it.deviceType to it }.toMap()
}

private fun Fragment.findPinView(@IdRes viewId: Int): TextView {
    return Ui.findView(this, viewId)
}
