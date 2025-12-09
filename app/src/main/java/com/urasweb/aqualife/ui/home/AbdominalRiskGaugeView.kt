package com.urasweb.aqualife.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class AbdominalRiskGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintSegment = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintMarker = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG)

    // perímetro abdominal en cm
    private var perimetro: Float = 0f

    init {
        paintMarker.color = Color.BLACK
        paintMarker.strokeWidth = 4f

        paintBorder.color = Color.DKGRAY
        paintBorder.style = Paint.Style.STROKE
        paintBorder.strokeWidth = 3f
    }

    fun setPerimetro(value: Float) {
        perimetro = value.coerceIn(60f, 140f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val top = h * 0.3f
        val bottom = h * 0.9f

        canvas.drawRect(0f, top, w, bottom, paintBorder)

        val minP = 60f
        val maxP = 140f

        fun xForP(v: Float): Float {
            val clamped = v.coerceIn(minP, maxP)
            return (clamped - minP) / (maxP - minP) * w
        }

        fun drawSegment(from: Float, to: Float, color: Int) {
            paintSegment.color = color
            val left = xForP(from)
            val right = xForP(to)
            canvas.drawRect(left, top, right, bottom, paintSegment)
        }

        // Rangos aproximados (puedes afinarlos luego por sexo):
        // Bajo riesgo
        drawSegment(60f, 80f, Color.parseColor("#A5D6A7"))   // verde
        // Riesgo moderado
        drawSegment(80f, 94f, Color.parseColor("#FFF59D"))   // amarillo
        // Riesgo alto
        drawSegment(94f, 140f, Color.parseColor("#EF9A9A"))  // rojo

        val markerX = xForP(perimetro)
        canvas.drawLine(markerX, h * 0.1f, markerX, h, paintMarker)
    }
}
