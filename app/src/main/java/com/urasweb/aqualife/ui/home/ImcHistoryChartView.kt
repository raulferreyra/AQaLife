package com.urasweb.aqualife.ui.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class ImcHistoryChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paintAxis = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintWeightLine = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintPerimeterBar = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintPoint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val paintText = Paint(Paint.ANTI_ALIAS_FLAG)

    private var weights: List<Float> = emptyList()
    private var perimeters: List<Float> = emptyList()

    init {
        paintAxis.color = Color.DKGRAY
        paintAxis.strokeWidth = 2f

        paintWeightLine.color = Color.parseColor("#1976D2")
        paintWeightLine.strokeWidth = 4f

        paintPerimeterBar.color = Color.parseColor("#FFB74D")

        paintPoint.color = Color.parseColor("#0D47A1")
        paintPoint.style = Paint.Style.FILL

        paintText.color = Color.DKGRAY
        paintText.textSize = 24f
    }

    fun setData(weightList: List<Float>, perimeterList: List<Float>) {
        val n = minOf(weightList.size, perimeterList.size, 5)
        weights = weightList.takeLast(n)
        perimeters = perimeterList.takeLast(n)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (weights.isEmpty() || perimeters.isEmpty()) {
            paintText.textAlign = Paint.Align.CENTER
            canvas.drawText("Sin registros", width / 2f, height / 2f, paintText)
            return
        }

        val left = paddingLeft.toFloat() + 40f
        val right = width - paddingRight.toFloat() - 20f
        val top = paddingTop.toFloat() + 20f
        val bottom = height - paddingBottom.toFloat() - 40f

        val chartWidth = right - left
        val chartHeight = bottom - top

        val n = weights.size
        val stepX = if (n > 1) chartWidth / (n - 1) else 0f

        val minWeight = weights.minOrNull() ?: 0f
        val maxWeight = weights.maxOrNull() ?: (minWeight + 1f)
        val minPer = perimeters.minOrNull() ?: 0f
        val maxPer = perimeters.maxOrNull() ?: (minPer + 1f)

        fun yForWeight(w: Float): Float {
            val range = (maxWeight - minWeight).let { if (it < 1f) 1f else it }
            val normalized = (w - minWeight) / range
            return bottom - normalized * (chartHeight * 0.6f)
        }

        fun barTopForPerimeter(p: Float): Float {
            val range = (maxPer - minPer).let { if (it < 1f) 1f else it }
            val normalized = (p - minPer) / range
            return bottom - normalized * (chartHeight * 0.4f)
        }

        // Eje X
        canvas.drawLine(left, bottom, right, bottom, paintAxis)

        // Barras de perímetro
        val barWidth = if (n > 1) stepX * 0.5f else chartWidth * 0.5f
        for (i in 0 until n) {
            val xCenter = left + i * stepX
            val barLeft = xCenter - barWidth / 2f
            val barRight = xCenter + barWidth / 2f
            val topBar = barTopForPerimeter(perimeters[i])
            canvas.drawRect(barLeft, topBar, barRight, bottom, paintPerimeterBar)
        }

        // Línea de peso
        var prevX = 0f
        var prevY = 0f
        for (i in 0 until n) {
            val x = left + i * stepX
            val y = yForWeight(weights[i])
            if (i > 0) {
                canvas.drawLine(prevX, prevY, x, y, paintWeightLine)
            }
            canvas.drawCircle(x, y, 6f, paintPoint)
            prevX = x
            prevY = y
        }

        // Leyenda
        paintText.textAlign = Paint.Align.LEFT
        canvas.drawText("Peso (línea)", left, top - 4f, paintText)
        canvas.drawText("Perímetro (barras)", left + 220f, top - 4f, paintText)
    }
}
