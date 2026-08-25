package com.satuart.tabletphotoframe.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.satuart.tabletphotoframe.R

class HoldRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val ringRadiusPx = resources.getDimension(R.dimen.hold_ring_radius)
    private val ringStrokeWidthPx = resources.getDimension(R.dimen.hold_ring_stroke_width)
    private val dotRadiusPx = resources.getDimension(R.dimen.hold_ring_dot_radius)

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringStrokeWidthPx
        strokeCap = Paint.Cap.ROUND
        color = ContextCompat.getColor(context, R.color.hold_ring_color)
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.hold_ring_color)
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringStrokeWidthPx
        color = ContextCompat.getColor(context, R.color.hold_ring_track_color)
    }
    private val trackBaseAlpha = Color.alpha(trackPaint.color)

    private val ringRect = RectF()

    private var contactX = 0f
    private var contactY = 0f
    private var ringProgress = 0f
    private var visualAlpha = 0f

    init {
        isClickable = false
        isFocusable = false
    }

    fun updateContact(x: Float, y: Float) {
        contactX = x
        contactY = y
        invalidate()
    }

    fun updateProgress(progress: Float, alpha: Float) {
        ringProgress = progress.coerceIn(0f, 1f)
        visualAlpha = alpha.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (visualAlpha <= 0f) return

        val alphaInt = (visualAlpha * 255).toInt()

        dotPaint.alpha = alphaInt
        canvas.drawCircle(contactX, contactY, dotRadiusPx, dotPaint)

        ringRect.set(
            contactX - ringRadiusPx,
            contactY - ringRadiusPx,
            contactX + ringRadiusPx,
            contactY + ringRadiusPx,
        )
        trackPaint.alpha = (trackBaseAlpha * visualAlpha).toInt()
        canvas.drawOval(ringRect, trackPaint)

        if (ringProgress > 0f) {
            ringPaint.alpha = alphaInt
            canvas.drawArc(ringRect, -90f, 360f * ringProgress, false, ringPaint)
        }
    }
}
