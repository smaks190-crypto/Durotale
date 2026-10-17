package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

enum class WaveType {
    SQUARE, TRIANGLE, SAWTOOTH, SINE
}

class RetroSoundEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val sampleRate = 22050

    var isMuted = false

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(durationMs: Long, amplitude: Int = 180) {
        if (isMuted) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun playTone(
        startFreq: Double,
        durationMs: Int,
        waveType: WaveType = WaveType.SQUARE,
        volume: Float = 0.6f,
        endFreq: Double? = null
    ) {
        if (isMuted) return
        scope.launch {
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt().coerceAtLeast(10)
                val buffer = ShortArray(numSamples)
                var phase = 0.0

                val targetEndFreq = endFreq ?: startFreq

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val currentFreq = startFreq + (targetEndFreq - startFreq) * progress
                    val phaseIncrement = 2.0 * PI * currentFreq / sampleRate
                    phase += phaseIncrement
                    if (phase >= 2.0 * PI) phase -= 2.0 * PI

                    // Volume envelope: quick attack, smooth exponential decay
                    val envelope = when {
                        progress < 0.08 -> progress / 0.08
                        else -> (1.0 - progress).coerceAtLeast(0.0)
                    }

                    val sampleValue: Double = when (waveType) {
                        WaveType.SQUARE -> if (phase < PI) 1.0 else -1.0
                        WaveType.TRIANGLE -> {
                            val normalized = phase / (2.0 * PI)
                            if (normalized < 0.5) {
                                4.0 * normalized - 1.0
                            } else {
                                3.0 - 4.0 * normalized
                            }
                        }
                        WaveType.SAWTOOTH -> {
                            1.0 - (phase / PI)
                        }
                        WaveType.SINE -> sin(phase)
                    }

                    val finalSample = (sampleValue * envelope * volume * Short.MAX_VALUE).toInt()
                    buffer[i] = finalSample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)
                audioTrack.play()

                delay(durationMs.toLong() + 20)
                audioTrack.stop()
                audioTrack.release()
            } catch (_: Exception) {}
        }
    }

    fun playVoiceBlip(speaker: String) {
        if (isMuted) return
        val randomOffset = (Math.random() * 20.0) - 10.0
        when (speaker) {
            "orderly" -> playTone(135.0 + randomOffset, 45, WaveType.SAWTOOTH, 0.45f)
            "player" -> playTone(360.0 + randomOffset, 35, WaveType.SQUARE, 0.4f)
            "vegetable" -> playTone(95.0 + randomOffset, 80, WaveType.SINE, 0.5f)
            else -> playTone(260.0 + randomOffset, 35, WaveType.SQUARE, 0.35f)
        }
    }

    fun sfxKnock() {
        playTone(110.0, 70, WaveType.TRIANGLE, 0.8f, 55.0)
        vibrate(30, 100)
        scope.launch {
            delay(130)
            playTone(115.0, 70, WaveType.TRIANGLE, 0.8f, 55.0)
            vibrate(30, 100)
        }
    }

    fun sfxBangDoor() {
        playTone(90.0, 150, WaveType.SAWTOOTH, 0.95f, 35.0)
        vibrate(120, 240)
        scope.launch {
            delay(110)
            playTone(75.0, 160, WaveType.SQUARE, 0.9f, 30.0)
        }
    }

    fun sfxScream() {
        playTone(420.0, 220, WaveType.SAWTOOTH, 0.7f, 680.0)
        vibrate(80, 180)
        scope.launch {
            delay(160)
            playTone(580.0, 240, WaveType.SAWTOOTH, 0.7f, 310.0)
        }
    }

    fun sfxDoorOpen() {
        playTone(180.0, 120, WaveType.SQUARE, 0.6f, 240.0)
        scope.launch {
            delay(110)
            playTone(240.0, 180, WaveType.SQUARE, 0.6f, 320.0)
        }
    }

    fun sfxSyringe() {
        playTone(620.0, 380, WaveType.SINE, 0.8f, 110.0)
        vibrate(180, 220)
        scope.launch {
            delay(320)
            playTone(220.0, 500, WaveType.SAWTOOTH, 0.7f, 65.0)
        }
    }

    fun sfxVictory() {
        val notes = listOf(261.6, 329.6, 392.0, 523.2)
        notes.forEachIndexed { i, freq ->
            scope.launch {
                delay(i * 130L)
                playTone(freq, 160, WaveType.SQUARE, 0.65f)
                vibrate(40, 120)
            }
        }
    }

    fun sfxBattleStart() {
        playTone(520.0, 70, WaveType.SQUARE, 0.8f)
        vibrate(60, 200)
        scope.launch {
            delay(110)
            playTone(520.0, 70, WaveType.SQUARE, 0.8f)
            delay(110)
            playTone(780.0, 220, WaveType.SAWTOOTH, 0.9f, 220.0)
            vibrate(120, 255)
        }
    }

    fun sfxSlash() {
        playTone(680.0, 90, WaveType.SAWTOOTH, 0.85f, 120.0)
        vibrate(70, 220)
        scope.launch {
            delay(80)
            playTone(140.0, 180, WaveType.SQUARE, 0.9f, 55.0)
        }
    }

    fun sfxDamage() {
        playTone(150.0, 140, WaveType.SAWTOOTH, 0.95f, 60.0)
        vibrate(90, 250)
    }

    fun sfxHeal() {
        playTone(392.0, 90, WaveType.SINE, 0.7f)
        scope.launch {
            delay(90)
            playTone(523.2, 120, WaveType.SINE, 0.7f)
            delay(100)
            playTone(659.2, 180, WaveType.SINE, 0.75f)
        }
    }

    fun sfxMenuSelect() {
        playTone(480.0, 50, WaveType.SQUARE, 0.5f)
    }

    fun sfxMenuMove() {
        playTone(320.0, 25, WaveType.SQUARE, 0.35f)
    }
}
