package io.particle.android.sdk.ui.devicelist

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.isVisible
import io.particle.android.sdk.cloud.ParticleDevice.ParticleDeviceType
import io.particle.android.sdk.ui.devicelist.DeviceTypeFilter.ARGON
import io.particle.android.sdk.ui.devicelist.DeviceTypeFilter.BORON
import io.particle.android.sdk.ui.devicelist.DeviceTypeFilter.ELECTRON
import io.particle.android.sdk.ui.devicelist.DeviceTypeFilter.OTHER
import io.particle.android.sdk.ui.devicelist.DeviceTypeFilter.PHOTON
import io.particle.android.sdk.ui.devicelist.DeviceTypeFilter.XENON
import io.particle.commonui.toDecorationColor
import io.particle.commonui.toDecorationLetter
import io.particle.sdk.app.R
import kotlinx.android.synthetic.main.view_device_type_filter_button.view.*


class DeviceTypeFilterButton : FrameLayout {

    var filter: DeviceTypeFilter
        get() = _filter
        set(value) {
            _filter = value
            updateFromNewFilter()
        }
    var isChecked: Boolean
        get() {
            return _checked
        }
        set(value) {
            _checked = value
            onCheckedChanged()
        }

    private var _filter: DeviceTypeFilter = ARGON
    private var _checked = false

    constructor(context: Context) : super(context) {
        init(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : super(
        context,
        attrs,
        defStyle
    ) {
        init(attrs, defStyle)
    }

    private fun init(attrs: AttributeSet?, defStyle: Int) {
        inflate(context, R.layout.view_device_type_filter_button, this)
    }

    private fun onCheckedChanged() {
        @DrawableRes val bg = if (_checked) {
            R.drawable.device_filter_button_background_selected
        } else {
            R.drawable.device_filter_button_background_unselected
        }
        setBackgroundResource(bg)
    }

    private fun updateFromNewFilter() {
        p_common_device_name.text = when (filter) {
            BORON -> "Boron / B SoM"
            ELECTRON -> "Electron / E SoM"
            ARGON -> "Argon"
            PHOTON -> "Photon / P1"
            XENON -> "Xenon"
            OTHER -> "Other"
        }

        if (filter == OTHER) {
            p_common_device_letter_circle.isVisible = false
            p_common_device_letter.isVisible = false
            return
        }

        val asDeviceType = when (filter) {
            BORON -> ParticleDeviceType.BORON
            ELECTRON -> ParticleDeviceType.ELECTRON
            ARGON -> ParticleDeviceType.ARGON
            PHOTON -> ParticleDeviceType.PHOTON
            XENON -> ParticleDeviceType.XENON
            OTHER -> ParticleDeviceType.OTHER
        }

        @ColorInt val colorValue: Int = ContextCompat.getColor(
            this.context,
            asDeviceType.toDecorationColor()
        )

        val bg = p_common_device_letter_circle.drawable
        bg.mutate()
        DrawableCompat.setTint(bg, colorValue)

        val letter = asDeviceType.toDecorationLetter()
        p_common_device_letter.text = letter
        p_common_device_letter.setTextColor(colorValue)
    }
}
