package com.aimsync.pro.ui

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import com.aimsync.pro.engine.DeviceProfile
import com.aimsync.pro.engine.GyroCalibrator
import com.aimsync.pro.engine.TouchCurveEngine
import com.aimsync.pro.system.SystemOptimizer
import kotlin.math.roundToInt

/**
 * Programmatic (no XML, no Compose) dashboard for AimSync Pro.
 * MainActivity only calls build() and stop().
 */
class AimSyncDashboard(private val activity: Activity) {

    private companion object {
        val BG = 0xFF0B0E14.toInt()
        val CARD = 0xFF151A24.toInt()
        val CYAN = 0xFF00F5D4.toInt()
        val PURPLE = 0xFF9B5DE5.toInt()
        val GREY = 0xFFAAAAAA.toInt()
        const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
        const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }

    private val density = activity.resources.displayMetrics.density
    private val profiles = DeviceProfile.values()
    private val calibrator = GyroCalibrator(activity)

    private lateinit var sourceSpinner: Spinner
    private lateinit var targetSpinner: Spinner
    private lateinit var etTpp: EditText
    private lateinit var etRedDot: EditText
    private lateinit var et3x: EditText
    private lateinit var etGyroNoScope: EditText
    private lateinit var etGyroRedDot: EditText
    private lateinit var etGyro3x: EditText
    private lateinit var fineLabel: TextView
    private lateinit var fineSeek: SeekBar
    private lateinit var resultView: TextView
    private lateinit var gyroProgress: ProgressBar
    private lateinit var gyroStatus: TextView
    private lateinit var touchRateText: TextView

    private var hasResult = false

    private fun dp(value: Int): Int = (value * density).roundToInt()

    // ---------- UI helpers ----------

    private fun makeText(content: String, size: Float, color: Int, bold: Boolean = false): TextView {
        val tv = TextView(activity)
        tv.text = content
        tv.textSize = size
        tv.setTextColor(color)
        if (bold) tv.setTypeface(null, Typeface.BOLD)
        return tv
    }

    private fun makeCard(title: String): LinearLayout {
        val card = LinearLayout(activity)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(dp(16), dp(14), dp(16), dp(14))
        val bg = GradientDrawable()
        bg.setColor(CARD)
        bg.cornerRadius = dp(14).toFloat()
        bg.setStroke(dp(1), 0x3300F5D4)
        card.background = bg
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.setMargins(0, 0, 0, dp(14))
        card.layoutParams = lp
        card.addView(makeText(title, 15f, CYAN, true))
        return card
    }

    private fun makeButton(label: String, color: Int, textColor: Int, onClick: () -> Unit): Button {
        val b = Button(activity)
        b.text = label
        b.setTextColor(textColor)
        val bg = GradientDrawable()
        bg.setColor(color)
        bg.cornerRadius = dp(10).toFloat()
        b.background = bg
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(8)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    private fun makeNumberField(parent: LinearLayout, label: String, default: Int): EditText {
        val caption = makeText(label, 12f, GREY)
        val captionLp = LinearLayout.LayoutParams(MATCH, WRAP)
        captionLp.topMargin = dp(8)
        caption.layoutParams = captionLp
        parent.addView(caption)

        val et = EditText(activity)
        et.setText(default.toString())
        et.setTextColor(CYAN)
        et.textSize = 16f
        et.inputType = InputType.TYPE_CLASS_NUMBER
        et.setSingleLine()
        parent.addView(et, LinearLayout.LayoutParams(MATCH, WRAP))
        return et
    }

    private fun makeProfileSpinner(selected: DeviceProfile): Spinner {
        val names = profiles.map { it.displayName }
        val adapter = ArrayAdapter(activity, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val spinner = Spinner(activity)
        spinner.adapter = adapter
        spinner.setSelection(selected.ordinal)
        return spinner
    }

    private fun readValue(et: EditText, default: Int): Int =
        (et.text.toString().trim().toIntOrNull() ?: default).coerceIn(1, 400)

    // ---------- Build ----------

    fun build(): View {
        val content = LinearLayout(activity)
        content.orientation = LinearLayout.VERTICAL
        content.setPadding(dp(16), dp(28), dp(16), dp(32))

        content.addView(makeText("\uD83C\uDFAF AIMSYNC PRO", 24f, CYAN, true))
        val subtitle = makeText("Oppo/realme vs iQOO touch sensitivity bridge", 13f, GREY)
        subtitle.setPadding(0, 0, 0, dp(18))
        content.addView(subtitle)

        // Device profiles
        val profileCard = makeCard("Device profiles")
        profileCard.addView(makeText("Convert FROM (device you know the values of)", 12f, GREY))
        sourceSpinner = makeProfileSpinner(DeviceProfile.IQOO_VIVO)
        profileCard.addView(sourceSpinner)
        val toLabel = makeText("Convert TO (device you are setting up)", 12f, GREY)
        toLabel.setPadding(0, dp(8), 0, 0)
        profileCard.addView(toLabel)
        targetSpinner = makeProfileSpinner(DeviceProfile.REALME_GT)
        profileCard.addView(targetSpinner)
        content.addView(profileCard)

        // Base sensitivity inputs
        val inputCard = makeCard("Source sensitivity values")
        etTpp = makeNumberField(inputCard, "TPP No Scope", 115)
        etRedDot = makeNumberField(inputCard, "Red Dot", 55)
        et3x = makeNumberField(inputCard, "3x Scope", 28)
        etGyroNoScope = makeNumberField(inputCard, "Gyro No Scope", 320)
        etGyroRedDot = makeNumberField(inputCard, "Gyro Red Dot", 300)
        etGyro3x = makeNumberField(inputCard, "Gyro 3x", 210)
        content.addView(inputCard)

        // Fine tune
        val fineCard = makeCard("Fine tune (\u00B110%)")
        fineLabel = makeText("Adjustment: 0%", 13f, Color.WHITE)
        fineLabel.setPadding(0, dp(6), 0, 0)
        fineCard.addView(fineLabel)
        fineSeek = SeekBar(activity)
        fineSeek.max = 20
        fineSeek.progress = 10
        fineSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pct = progress - 10
                fineLabel.text = "Adjustment: " + (if (pct > 0) "+" else "") + pct + "%"
                if (hasResult) recalc()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        fineCard.addView(fineSeek, LinearLayout.LayoutParams(MATCH, WRAP))
        fineCard.addView(
            makeText(
                "Conversion factors are estimates, not official values. Adjust by feel in training mode.",
                11f, GREY
            )
        )
        content.addView(fineCard)

        // Convert + result
        content.addView(makeButton("CALIBRATE & CONVERT", PURPLE, Color.WHITE) { recalc() })
        val resultCard = makeCard("Recommended values")
        val lp = resultCard.layoutParams as LinearLayout.LayoutParams
        lp.topMargin = dp(14)
        resultView = makeText("--", 14f, Color.WHITE)
        resultView.typeface = Typeface.MONOSPACE
        resultView.setPadding(0, dp(8), 0, 0)
        resultCard.addView(resultView)
        content.addView(resultCard)

        // Gyro
        val gyroCard = makeCard("Gyroscope calibration")
        gyroProgress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal)
        gyroProgress.max = 100
        val gp = LinearLayout.LayoutParams(MATCH, WRAP)
        gp.topMargin = dp(8)
        gyroCard.addView(gyroProgress, gp)
        gyroStatus = makeText("Keep the phone flat and still, then tap Calibrate.", 13f, Color.WHITE)
        gyroCard.addView(gyroStatus)
        gyroCard.addView(makeButton("CALIBRATE GYRO", CYAN, Color.BLACK) { startGyro() })
        content.addView(gyroCard)

        // Touch rate
        val touchCard = makeCard("Live touch sampling rate")
        touchRateText = makeText("-- Hz", 22f, CYAN, true)
        touchRateText.setPadding(0, dp(6), 0, dp(6))
        touchCard.addView(touchRateText)
        val touchView = TouchRateView(activity)
        val tvBg = GradientDrawable()
        tvBg.setColor(0xFF0B0E14.toInt())
        tvBg.cornerRadius = dp(10).toFloat()
        touchView.background = tvBg
        touchView.onRate = { hz -> touchRateText.text = "$hz Hz" }
        touchCard.addView(touchView, LinearLayout.LayoutParams(MATCH, dp(140)))
        touchCard.addView(
            makeText("Approximate value as delivered by the Android input system.", 11f, GREY)
        )
        content.addView(touchCard)

        // System shortcuts
        val sysCard = makeCard("System shortcuts")
        sysCard.addView(makeButton("POINTER / INPUT SETTINGS", PURPLE, Color.WHITE) {
            SystemOptimizer.openPointerSpeedSettings(activity)
        })
        sysCard.addView(makeButton("TOUCH & HOLD DELAY", PURPLE, Color.WHITE) {
            SystemOptimizer.openTouchDelaySettings(activity)
        })
        sysCard.addView(makeButton("GAMING DND ON / OFF", PURPLE, Color.WHITE) {
            SystemOptimizer.toggleGamingDnd(activity)
        })
        content.addView(sysCard)

        content.addView(
            makeText(
                "AimSync Pro does not interact with the game process, memory or screen. " +
                    "It only calculates values and opens system settings.",
                11f, GREY
            )
        )

        val scroll = ScrollView(activity)
        scroll.setBackgroundColor(BG)
        scroll.isFillViewport = true
        scroll.addView(content)
        return scroll
    }

    fun stop() {
        calibrator.stop()
    }

    // ---------- Logic ----------

    private fun recalc() {
        val source = profiles[sourceSpinner.selectedItemPosition]
        val target = profiles[targetSpinner.selectedItemPosition]

        val r = TouchCurveEngine.convertSensitivity(
            activity,
            source,
            target,
            readValue(etTpp, 115),
            readValue(etRedDot, 55),
            readValue(et3x, 28),
            readValue(etGyroNoScope, 320),
            readValue(etGyroRedDot, 300),
            readValue(etGyro3x, 210)
        )

        val mult = 1f + (fineSeek.progress - 10) / 100f
        fun touch(v: Int): Int = (v * mult).roundToInt().coerceIn(1, 300)
        fun gyro(v: Int): Int = (v * mult).roundToInt().coerceIn(1, 400)

        resultView.text =
            "TPP No Scope : ${touch(r.tppNoScope)}%\n" +
            "Red Dot      : ${touch(r.redDot)}%\n" +
            "2x Scope     : ${touch(r.scope2x)}%\n" +
            "3x Scope     : ${touch(r.scope3x)}%\n" +
            "4x Scope     : ${touch(r.scope4x)}%\n" +
            "Gyro No Scope: ${gyro(r.gyroNoScope)}%\n" +
            "Gyro Red Dot : ${gyro(r.gyroRedDot)}%\n" +
            "Gyro 3x      : ${gyro(r.gyro3x)}%"
        hasResult = true
    }

    private fun startGyro() {
        val sm = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        if (sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE) == null) {
            gyroStatus.text = "No gyroscope found on this device."
            return
        }
        gyroProgress.progress = 0
        gyroStatus.text = "Calibrating... keep the phone flat and still."
        calibrator.startCalibration { progress, drift, done ->
            activity.runOnUiThread {
                gyroProgress.progress = (progress * 100f).roundToInt()
                if (done) {
                    val rating = when {
                        drift < 0.01f -> "very stable"
                        drift < 0.03f -> "acceptable"
                        else -> "high drift, try again on a flat surface"
                    }
                    gyroStatus.text = "Done. Drift: " + "%.4f".format(drift) + " rad/s (" + rating + ")"
                }
            }
        }
    }
}
