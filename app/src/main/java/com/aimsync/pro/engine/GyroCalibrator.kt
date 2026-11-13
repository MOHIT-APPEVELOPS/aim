package com.aimsync.pro.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class GyroCalibrator(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    private var sampleCount = 0
    private var sumX = 0f
    private var sumY = 0f
    private var sumZ = 0f

    var biasX = 0f
        private set
    var biasY = 0f
        private set
    var biasZ = 0f
        private set

    private var onProgress: ((progress: Float, driftMagnitude: Float, done: Boolean) -> Unit)? = null

    fun startCalibration(onUpdate: (progress: Float, driftMagnitude: Float, done: Boolean) -> Unit) {
        if (gyroSensor == null) {
            onUpdate(1f, 0f, true)
            return
        }
        sampleCount = 0
        sumX = 0f
        sumY = 0f
        sumZ = 0f
        onProgress = onUpdate
        sensorManager.registerListener(this, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_GYROSCOPE) return

        val targetSamples = 120
        if (sampleCount < targetSamples) {
            sumX += event.values[0]
            sumY += event.values[1]
            sumZ += event.values[2]
            sampleCount++

            biasX = sumX / sampleCount
            biasY = sumY / sampleCount
            biasZ = sumZ / sampleCount

            val magnitude = sqrt(biasX * biasX + biasY * biasY + biasZ * biasZ)
            val progress = sampleCount.toFloat() / targetSamples
            val isDone = sampleCount >= targetSamples

            onProgress?.invoke(progress, magnitude, isDone)
            if (isDone) stop()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
