package io.particle.mesh.setup.ui.views

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import io.particle.mesh.R


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
        blackTransparency = resources.getColor(R.color.p_mesh_black_sorta_transparent, null)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed) {
            recalculate()
        }
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

    private fun recalculate() {
        val scaleFactor = Resources.getSystem().displayMetrics.density
        val rekt = Rect(0, 0, this.width, this.height)
        this.circleRect = getRektFForFrameOverlay(rekt)
        this.radius = 5.0f * scaleFactor
        // Redraw after defining circle
        postInvalidate()
    }

    private fun getRektFForFrameOverlay(rect: Rect): RectF {
        val scaleFactor = Resources.getSystem().displayMetrics.density
        val size = 40 * scaleFactor
        return RectF(
                rect.centerX() - size,
                rect.centerY() - size,
                rect.centerX() + size,
                rect.centerY() + size
        )
    }

}