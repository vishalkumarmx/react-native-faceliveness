package com.faceliveness

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class LivenessOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        color = Color.GREEN
    }

    private var previewWidth: Int = 0
    private var previewHeight: Int = 0
    private var factorX: Float = 1f
    private var factorY: Float = 1f

    private var showBox: Boolean = false
    private var boxRect: RectF = RectF()

    fun setPreviewSize(width: Int, height: Int) {
        previewWidth = width
        previewHeight = height
        updateScale()
    }

    fun updateResult(result: LivenessResult, threshold: Float) {
        if (!result.hasFace) {
            showBox = false
            invalidate()
            return
        }

        // Map model coords (preview) to view coords (portrait)
        val left = result.left * factorX
        val top = result.top * factorY
        val right = result.right * factorX
        val bottom = result.bottom * factorY
        boxRect.set(left, top, right, bottom)

        boxPaint.color = if (result.confidence >= threshold) {
            Color.parseColor("#22c55e")
        } else {
            Color.parseColor("#ef4444")
        }

        showBox = true
        invalidate()
    }

    fun clear() {
        showBox = false
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateScale()
    }

    private fun updateScale() {
        if (previewWidth <= 0 || previewHeight <= 0 || width <= 0 || height <= 0) {
            return
        }
        // Match the working sample: portrait view maps to rotated preview.
        factorX = width / previewHeight.toFloat()
        factorY = height / previewWidth.toFloat()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (showBox) {
            canvas.drawRect(boxRect, boxPaint)
        }
    }
}
