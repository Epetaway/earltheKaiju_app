package com.earlthekaiju.coach.feature.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.earlthekaiju.coach.domain.timer.TimerCue
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Round cues made in code: a struck-bell sound (three inharmonic partials with exponential decay),
 * short wood-block clicks for the warning, and plain beeps for the 3-2-1 count-in. No audio files,
 * so there is nothing to license. Played on the media stream so a gym Bluetooth speaker gets them.
 *
 * Patterns: round start = two bell strikes · rest = one low bell · warning = three clicks ·
 * count-in = one beep per second · finished = three bells.
 */
class ToneCuePlayer(context: Context) : CuePlayer {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val audioExecutor = Executors.newSingleThreadExecutor { r -> Thread(r, "kaiju-cues").apply { isDaemon = true } }
    private val cache = HashMap<TimerCue, ShortArray>()

    override fun play(cue: TimerCue, sound: Boolean, vibration: Boolean) {
        if (vibration) vibrate(cue)
        if (sound) audioExecutor.execute { runCatching { playPcm(pcmFor(cue)) } }
    }

    private fun vibrate(cue: TimerCue) {
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        val pattern = when (cue) {
            TimerCue.ROUND_START -> longArrayOf(0, 220, 120, 220)
            TimerCue.ROUND_END -> longArrayOf(0, 500)
            TimerCue.WARNING -> longArrayOf(0, 80, 80, 80, 80, 80)
            TimerCue.COUNTDOWN_TICK -> longArrayOf(0, 40)
            TimerCue.FINISHED -> longArrayOf(0, 300, 150, 300, 150, 300)
        }
        runCatching { v.vibrate(VibrationEffect.createWaveform(pattern, -1)) }
    }

    private fun pcmFor(cue: TimerCue): ShortArray = synchronized(cache) {
        cache.getOrPut(cue) {
            when (cue) {
                TimerCue.ROUND_START -> mix(listOf(0.0 to bell(1320.0, 1.1), 0.22 to bell(1320.0, 1.1)))
                TimerCue.ROUND_END -> mix(listOf(0.0 to bell(880.0, 1.6)))
                TimerCue.WARNING -> mix(listOf(0.0 to click(), 0.16 to click(), 0.32 to click()))
                TimerCue.COUNTDOWN_TICK -> beep(1000.0, 0.09)
                TimerCue.FINISHED -> mix(listOf(0.0 to bell(1320.0, 1.2), 0.3 to bell(1320.0, 1.2), 0.6 to bell(990.0, 1.8)))
            }
        }
    }

    private fun playPcm(pcm: ShortArray) {
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        try {
            track.write(pcm, 0, pcm.size)
            track.play()
            Thread.sleep(pcm.size * 1000L / SAMPLE_RATE + 50)
        } finally {
            track.release()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 44_100

        fun bell(freq: Double, seconds: Double): DoubleArray {
            val n = (SAMPLE_RATE * seconds).toInt()
            val partials = listOf(1.0 to 1.0, 2.76 to 0.45, 5.40 to 0.22, 8.93 to 0.08)
            return DoubleArray(n) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val attack = (t / 0.004).coerceAtMost(1.0)
                partials.sumOf { (ratio, amp) -> amp * exp(-t * (2.2 + ratio * 0.9)) * sin(2 * PI * freq * ratio * t) } * attack
            }
        }

        fun click(): DoubleArray {
            val n = (SAMPLE_RATE * 0.06).toInt()
            return DoubleArray(n) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                exp(-t * 70) * (sin(2 * PI * 1900 * t) * 0.7 + sin(2 * PI * 3100 * t) * 0.3)
            }
        }

        fun beep(freq: Double, seconds: Double): ShortArray {
            val n = (SAMPLE_RATE * seconds).toInt()
            val wave = DoubleArray(n) { i ->
                val t = i.toDouble() / SAMPLE_RATE
                val env = minOf(1.0, t / 0.005, (seconds - t) / 0.01).coerceAtLeast(0.0)
                env * sin(2 * PI * freq * t)
            }
            return toPcm(wave)
        }

        fun mix(parts: List<Pair<Double, DoubleArray>>): ShortArray {
            val length = parts.maxOf { (offset, wave) -> (offset * SAMPLE_RATE).toInt() + wave.size }
            val out = DoubleArray(length)
            for ((offset, wave) in parts) {
                val start = (offset * SAMPLE_RATE).toInt()
                for (i in wave.indices) out[start + i] += wave[i]
            }
            return toPcm(out)
        }

        fun toPcm(wave: DoubleArray): ShortArray {
            val peak = wave.maxOf { kotlin.math.abs(it) }.coerceAtLeast(1e-6)
            val gain = 0.85 / peak
            return ShortArray(wave.size) { i -> (wave[i] * gain * Short.MAX_VALUE).toInt().toShort() }
        }
    }
}
