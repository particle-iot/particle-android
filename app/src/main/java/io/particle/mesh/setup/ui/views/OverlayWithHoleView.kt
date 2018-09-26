package io.particle.mesh.setup.ui.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import io.particle.sdk.app.R


class OverlayWithHoleView(
        context: Context,
        attrs: AttributeSet
) : ImageView(context, attrs) {

    private var circleRect: RectF? = null
    private var radius: Float = 0.0f

    private val porterDuffClearMode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var blackTransparency: Int = 0

    init {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        blackTransparency = resources.getColor(R.color.black_semi_transparent)
    }

    fun setCircle(rect: RectF, radius: Float) {
        this.circleRect = rect
        this.radius = radius
        //Redraw after defining circle
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val crect = circleRect
        if (crect != null) {
            paint.xfermode = null
            paint.color = blackTransparency
            paint.style = Paint.Style.FILL
            canvas.drawPaint(paint)

            paint.xfermode = porterDuffClearMode
            canvas.drawRoundRect(crect, radius, radius, paint)
        }
    }
}