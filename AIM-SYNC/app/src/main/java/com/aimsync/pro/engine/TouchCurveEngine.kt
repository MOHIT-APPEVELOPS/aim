package com.aimsync.pro.engine

import android.content.Context
import kotlin.math.roundToInt

enum class DeviceProfile(
    val displayName: String,
    val accelerationFactor: Float, // 1.0 = Linear (iQOO/Vivo), >1.0 = Non-linear acceleration (Oppo/realme)
    val baseSamplingHz: Int
) {
    IQOO_VIVO("iQOO / Vivo (Funtouch / OriginOS)", 1.00f, 360),
    REALME_GT("realme (realme UI)", 1.14f, 360),
    OPPO_COLOROS("Oppo (ColorOS)", 1.16f, 240)
}

data class SensitivityResult(
    val tppNoScope: Int,
    val redDot: Int,
    val scope2x: Int,
    val scope3x: Int,
    val scope4x: Int,
    val gyroNoScope: Int,
    val gyroRedDot: Int,
    val gyro3x: Int
)

object TouchCurveEngine {

    /**
     * Converts sensitivity from a source device profile to target device profile
     * compensating for OS touch acceleration curves and screen pixel density.
     */
    fun convertSensitivity(
        context: Context,
        source: DeviceProfile,
        target: DeviceProfile,
        baseTpp: Int,
        baseRedDot: Int,
        base3x: Int,
        baseGyroNoScope: Int,
        baseGyroRedDot: Int,
        baseGyro3x: Int
    ): SensitivityResult {
        val metrics = context.resources.displayMetrics
        // Normalize screen width ratio relative to standard 1080p gaming baseline
        val widthPx = minOf(metrics.widthPixels, metrics.heightPixels).coerceAtLeast(720)
        val dpiScale = 1080f / widthPx.toFloat()

        // Compensation ratio between source linear response and target acceleration curve
        val curveRatio = source.accelerationFactor / target.accelerationFactor
        val finalTouchMultiplier = curveRatio * (0.85f + (0.15f * dpiScale))

        // Gyroscope polling compensation (Oppo/realme often apply slight smoothing filter)
        val gyroMultiplier = if (source == DeviceProfile.IQOO_VIVO && target != DeviceProfile.IQOO_VIVO) {
            1.07f // +7% boost to overcome ColorOS/realme UI micro-smoothing
        } else if (source != DeviceProfile.IQOO_VIVO && target == DeviceProfile.IQOO_VIVO) {
            0.93f
        } else {
            1.00f
        }

        return SensitivityResult(
            tppNoScope = (baseTpp * finalTouchMultiplier).roundToInt().coerceIn(1, 300),
            redDot = (baseRedDot * finalTouchMultiplier).roundToInt().coerceIn(1, 300),
            scope2x = ((baseRedDot * 0.65f) * finalTouchMultiplier).roundToInt().coerceIn(1, 300),
            scope3x = (base3x * finalTouchMultiplier).roundToInt().coerceIn(1, 300),
            scope4x = ((base3x * 0.72f) * finalTouchMultiplier).roundToInt().coerceIn(1, 300),
            gyroNoScope = (baseGyroNoScope * gyroMultiplier).roundToInt().coerceIn(1, 400),
            gyroRedDot = (baseGyroRedDot * gyroMultiplier).roundToInt().coerceIn(1, 400),
            gyro3x = (baseGyro3x * gyroMultiplier).roundToInt().coerceIn(1, 400)
        )
    }
}
