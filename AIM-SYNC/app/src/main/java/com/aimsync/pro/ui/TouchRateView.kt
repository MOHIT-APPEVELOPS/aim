package com.aimsync.pro.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View

/**
 * Measures how many touch samples per second the system delivers while a finger
 * slides across this view. Historical (batched) samples are counted too.
 */
class TouchRateView(context: Context) : View(context) {

    var onRate: ((Int) -> Unit)? = null

    private var windowStartMs = 0L
    private var sampleCount = 0

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8899AA.toInt()
        textSize = 14f * resources.displayMetrics.density
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawText(
            "Slide your finger here",
            width / 2f,
            height / 2f,
            paint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                windowStartMs = event.eventTime
                sampleCount = 0
            }
            MotionEvent.ACTION_MOVE -> {
                sampleCount += event.historySize + 1
                val elapsed = event.eventTime - windowStartMs
                if (elapsed >= 300L) {
                    val hz = (sampleCount * 1000L / elapsed).toInt()
                    onRate?.invoke(hz)
                    windowStartMs = event.eventTime
                    sampleCount = 0
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }
}
