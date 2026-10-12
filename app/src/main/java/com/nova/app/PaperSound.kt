package com.nova.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import kotlin.math.exp
import kotlin.math.pow
import kotlin.random.Random

/** Synthesises a "paper sliding / page turning" swish at runtime, so no sound asset is required. */
object PaperSound {
    private const val SR = 22050
    private var next = 0
    private val handler = Handler(Looper.getMainLooper())

    private val variants: List<ShortArray> by lazy { listOf(gen(1, 0.46f), gen(2, 0.40f), gen(3, 0.52f)) }

    private fun gen(seed: Int, dur: Float): ShortArray {
        val n = (SR * dur).toInt()
        val rnd = Random(seed)
        val out = ShortArray(n)
        var lp1 = 0f
        var lp2 = 0f
        for (i in 0 until n) {
            val t = i / n.toFloat()
            // quick rise, long fall, plus a soft "page settles" bump near the end
            val env = (if (t < 0.14f) (t / 0.14f).pow(1.5f) else exp(-(t - 0.14f) * 5.2f)) +
                0.22f * exp(-(((t - 0.74f) / 0.06f).pow(2)))
            val noise = rnd.nextFloat() * 2f - 1f
            lp1 += 0.46f * (noise - lp1)          // roll off the highs
            lp2 += 0.035f * (lp1 - lp2)           // slow follower
            var s = (lp1 - lp2) * env * 1.9f      // band-pass = papery hiss
            if (rnd.nextFloat() < 0.012f) s += (rnd.nextFloat() - 0.5f) * 0.9f * env // fibre crackle
            out[i] = (s.coerceIn(-1f, 1f) * 0.85f * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    fun play() {
        try {
            val buf = variants[next++ % variants.size]
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buf.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(buf, 0, buf.size)
            track.setVolume(0.9f)
            track.play()
            handler.postDelayed({ try { track.release() } catch (_: Exception) {} }, (buf.size * 1000L / SR) + 250L)
        } catch (_: Exception) {
        }
    }
}
