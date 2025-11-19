package com.crediweb.aqualife.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ImcGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintSegment = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintMarker = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintBorder = Paint(Paint.ANTI_ALIAS_FLAG)

    private var imc: Double = 0.0

    init {
        paintMarker.color = Color.BLACK
        paintMarker.strokeWidth = 4f

        paintBorder.color = Color.DKGRAY
        paintBorder.style = Paint.Style.STROKE
        paintBorder.strokeWidth = 3f
    }

    fun setImc(value: Double) {
        imc = value.coerceIn(10.0, 45.0) // limitamos rango razonable
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val top = h * 0.4f
        val bottom = h * 0.9f

        // Fondo con borde
        canvas.drawRect(0f, top, w, bottom, paintBorder)

        // Rangos de IMC (usamos 10 a 40 como rango visual)
        val minImc = 10f
        val maxImc = 40f

        fun xForImc(v: Float): Float {
            val clamped = v.coerceIn(minImc, maxImc)
            return (clamped - minImc) / (maxImc - minImc) * w
        }

        // Dibujar segmentos (colores de ejemplo)
        fun drawSegment(fromImc: Float, toImc: Float, color: Int) {
            paintSegment.color = color
            val left = xForImc(fromImc)
            val right = xForImc(toImc)
            canvas.drawRect(left, top, right, bottom, paintSegment)
        }

        // Rango bajo peso
        drawSegment(10f, 18.5f, Color.parseColor("#FFE082")) // amarillo claro
        // Normal
        drawSegment(18.5f, 24.9f, Color.parseColor("#A5D6A7")) // verde
        // Sobrepeso
        drawSegment(24.9f, 29.9f, Color.parseColor("#FFF59D")) // amarillo
        // Obesidad I
        drawSegment(29.9f, 34.9f, Color.parseColor("#FFCC80")) // naranja
        // Obesidad II / III
        drawSegment(34.9f, 40f, Color.parseColor("#EF9A9A")) // rojo suave

        // Marcador del IMC
        val markerX = xForImc(imc.toFloat())
        canvas.drawLine(markerX, h * 0.2f, markerX, h, paintMarker)
    }

}