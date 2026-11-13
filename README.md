# 🎯 AimSync Pro — Touch & Gyro Sensitivity Bridge for BGMI

**AimSync Pro** is an Android utility that helps convert sensitivity values between **ColorOS / realme UI (Oppo, realme)** and **Funtouch OS / OriginOS (Vivo, iQOO)**.

### What it does
- Converts TPP / red dot / scope / gyro sensitivity values between device profiles using a math formula.
- Fine-tune slider (±10%) to adjust the result by feel.
- Gyroscope bias calibration using Android `SensorManager`.
- Live touch sampling rate (Hz) meter.
- Shortcuts to system settings (input/pointer, touch & hold delay, Do Not Disturb).

### What it does NOT do
AimSync Pro does not hook, inject into, read, or overlay the game process. It only calculates values and opens system settings.

> The conversion factors are estimates, not official manufacturer values. Always verify in training mode.

### Tech
Kotlin, plain Android Views (no Compose, no AppCompat), AGP 8.2.0, Kotlin 1.9.20, Gradle 8.4, JDK 17, minSdk 26, targetSdk 34.

### Build (GitHub Actions)
Push to `main` or run **Actions → Build Android APK → Run workflow**, then download the `AimSync-Pro-Debug` artifact.
