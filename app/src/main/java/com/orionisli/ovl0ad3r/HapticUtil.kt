package com.orionisli.ovl0ad3r

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RequiresApi

object HapticUtil {

    private const val PRIMITIVE_LOW_TICK = 0
    private const val PRIMITIVE_TICK = 1
    private const val PRIMITIVE_CLICK = 2
    private const val PRIMITIVE_THUD = 3

    private var appContext: Context? = null
    private var vibrator: Vibrator? = null
    private var amplitudeControl: Boolean = false

    var strength: Int = 2

    fun init(context: Context) {
        val app = context.applicationContext
        appContext = app
        val sp = app.getSharedPreferences("ovl0ad3r_settings", Context.MODE_PRIVATE)
        strength = sp.getInt("haptic_strength", 2)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        amplitudeControl = vibrator?.hasAmplitudeControl() == true
    }

    private fun getVibrator(): Vibrator? {
        if (vibrator == null && appContext != null) {
            init(appContext!!)
        }
        return vibrator
    }

    fun preview(targetStrength: Int) {
        val vib = getVibrator() ?: return
        if (!vib.hasVibrator()) return
        if (targetStrength <= 0) return

        when (targetStrength) {
            1 -> {
                vibrateWithCustomScale(
                    ms = 8,
                    amplitude = 40,
                    primitiveCode = PRIMITIVE_LOW_TICK,
                    scale = 0.22f
                )
            }
            2 -> {
                vibrateWithCustomScale(
                    ms = 18,
                    amplitude = 135,
                    primitiveCode = PRIMITIVE_CLICK,
                    scale = 0.65f
                )
            }
            3 -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    vib.areAllPrimitivesSupported(
                        VibrationEffect.Composition.PRIMITIVE_QUICK_RISE,
                        VibrationEffect.Composition.PRIMITIVE_CLICK
                    )
                ) {
                    try {
                        val comp = VibrationEffect.startComposition()
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_QUICK_RISE, 0.75f)
                            .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 1.0f, 12)
                            .compose()
                        dispatchEffect(comp)
                        return
                    } catch (_: Exception) {}
                }
                vibrateWithCustomScale(
                    ms = 40,
                    amplitude = 255,
                    primitiveCode = PRIMITIVE_THUD,
                    scale = 1.0f
                )
            }
        }
    }

    fun detentTick() {
        if (strength == 0) return
        val amp = when (strength) {
            1 -> 15
            3 -> 65
            else -> 32
        }
        val scale = when (strength) {
            1 -> 0.06f
            3 -> 0.28f
            else -> 0.14f
        }
        vibrateWithCustomScale(3, amp, PRIMITIVE_LOW_TICK, scale)
    }

    fun tick() {
        if (strength == 0) return
        val amp = when (strength) {
            1 -> 40
            3 -> 160
            else -> 90
        }
        val scale = when (strength) {
            1 -> 0.22f
            3 -> 0.75f
            else -> 0.45f
        }
        vibrateWithCustomScale(10, amp, PRIMITIVE_TICK, scale)
    }

    fun click() {
        if (strength == 0) return
        val amp = when (strength) {
            1 -> 60
            3 -> 220
            else -> 135
        }
        val scale = when (strength) {
            1 -> 0.35f
            3 -> 0.90f
            else -> 0.65f
        }
        vibrateWithCustomScale(16, amp, PRIMITIVE_CLICK, scale)
    }

    fun heavyClick() {
        if (strength == 0) return
        val amp = when (strength) {
            1 -> 85
            3 -> 255
            else -> 180
        }
        val scale = when (strength) {
            1 -> 0.45f
            3 -> 1.0f
            else -> 0.80f
        }
        vibrateWithCustomScale(30, amp, PRIMITIVE_THUD, scale)
    }

    fun confirm() {
        if (strength == 0) return
        val amp = when (strength) { 1 -> 80; 3 -> 240; else -> 160 }
        vibrateOneShot(24, amp)
    }

    fun warning() {
        if (strength == 0) return
        val amp = when (strength) { 1 -> 100; 3 -> 255; else -> 200 }
        vibrateOneShot(35, amp)
    }

    fun success() {
        if (strength == 0) return
        val vib = getVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val a1 = when (strength) { 1 -> 70; 3 -> 220; else -> 140 }
            val a2 = when (strength) { 1 -> 110; 3 -> 255; else -> 200 }
            val timings = longArrayOf(0, 16, 45, 22)
            val amplitudes = intArrayOf(0, a1, 0, a2)
            try {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                dispatchEffect(effect)
                return
            } catch (_: Exception) {}
        }
        vibrateOneShot(25, 180)
    }

    fun error() {
        if (strength == 0) return
        val vib = getVibrator() ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val a = when (strength) { 1 -> 120; 3 -> 255; else -> 220 }
            val timings = longArrayOf(0, 30, 50, 40)
            val amplitudes = intArrayOf(0, a, 0, a)
            try {
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                dispatchEffect(effect)
                return
            } catch (_: Exception) {}
        }
        vibrateOneShot(45, 255)
    }

    private fun vibrateWithCustomScale(ms: Long, amplitude: Int, primitiveCode: Int, scale: Float) {
        val vib = getVibrator() ?: return
        if (!vib.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val primitive = primitiveId(primitiveCode)
            if (vib.areAllPrimitivesSupported(primitive)) {
                try {
                    val effect = VibrationEffect.startComposition()
                        .addPrimitive(primitive, scale.coerceIn(0.05f, 1.0f))
                        .compose()
                    dispatchEffect(effect)
                    return
                } catch (_: Exception) {
                }
            }
        }

        vibrateOneShot(ms, amplitude)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun primitiveId(code: Int): Int = when (code) {
        PRIMITIVE_LOW_TICK -> VibrationEffect.Composition.PRIMITIVE_LOW_TICK
        PRIMITIVE_TICK -> VibrationEffect.Composition.PRIMITIVE_TICK
        PRIMITIVE_CLICK -> VibrationEffect.Composition.PRIMITIVE_CLICK
        else -> VibrationEffect.Composition.PRIMITIVE_THUD
    }

    private fun dispatchEffect(effect: VibrationEffect) {
        val vib = getVibrator() ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attrs = android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                    .build()
                vib.vibrate(effect, attrs)
            } else {
                vib.vibrate(effect)
            }
        } catch (_: Exception) {
            try {
                vib.vibrate(effect)
            } catch (_: Exception) {}
        }
    }

    private fun vibrateOneShot(ms: Long, amplitude: Int) {
        val vib = getVibrator() ?: return
        if (!vib.hasVibrator()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amp = if (amplitudeControl) {
                    amplitude.coerceIn(1, 255)
                } else {
                    VibrationEffect.DEFAULT_AMPLITUDE
                }
                val effect = VibrationEffect.createOneShot(ms.coerceAtLeast(1L), amp)
                dispatchEffect(effect)
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(ms)
            }
        } catch (_: Exception) {
        }
    }
}
