/**
 * presentation/voice/VoiceRecorder.kt
 * Запись голосовых (Telegram-style): AAC в .m4a с расчётом waveform
 * на 100 точек из MediaRecorder.maxAmplitude.
 */
package com.whatsmax.presentation.voice

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.*
import java.io.File

data class VoiceRecordResult(
    val file: File,
    val durationMs: Long,
    val waveform: List<Int> // ровно 100 точек, амплитуды 0..100
)

class VoiceRecorder(private val context: Context) {

    companion object {
        const val WAVEFORM_POINTS = 100
        private const val SAMPLE_INTERVAL_MS = 50L  // сэмплируем амплитуду каждые 50 мс
    }

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startedAt: Long = 0L
    private val samples = mutableListOf<Int>()
    private var samplerJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start() {
        if (recorder != null) return
        val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        outputFile = file
        samples.clear()

        val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context)
                  else @Suppress("DEPRECATION") MediaRecorder()

        rec.setAudioSource(MediaRecorder.AudioSource.MIC)
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        rec.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        rec.setAudioEncodingBitRate(64_000)
        rec.setAudioSamplingRate(44_100)
        rec.setOutputFile(file.absolutePath)
        rec.prepare()
        rec.start()
        recorder = rec
        startedAt = SystemClock.elapsedRealtime()

        // Сэмплер амплитуды
        samplerJob = scope.launch {
            while (isActive) {
                val amp = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)
                samples.add(amp)
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    /** Останавливает запись и возвращает результат. После вызова инстанс непригоден. */
    fun stop(): VoiceRecordResult? {
        val rec = recorder ?: return null
        val file = outputFile ?: return null
        samplerJob?.cancel()
        runCatching { rec.stop() }
        runCatching { rec.release() }
        recorder = null
        val durationMs = SystemClock.elapsedRealtime() - startedAt
        return VoiceRecordResult(
            file       = file,
            durationMs = durationMs,
            waveform   = normalize(samples)
        )
    }

    /** Отмена записи: удаляет файл, ничего не возвращает. */
    fun cancel() {
        samplerJob?.cancel()
        val rec = recorder
        recorder = null
        runCatching { rec?.stop() }
        runCatching { rec?.release() }
        outputFile?.delete()
        outputFile = null
    }

    /** Свернуть произвольное число сэмплов в ровно 100 точек 0..100 (по среднему в бакете). */
    private fun normalize(raw: List<Int>): List<Int> {
        if (raw.isEmpty()) return List(WAVEFORM_POINTS) { 0 }
        val max = (raw.max().coerceAtLeast(1)).toFloat()
        val bucketSize = raw.size.toFloat() / WAVEFORM_POINTS
        return List(WAVEFORM_POINTS) { i ->
            val from = (i * bucketSize).toInt()
            val to   = ((i + 1) * bucketSize).toInt().coerceAtMost(raw.size)
            val slice = if (from < to) raw.subList(from, to) else listOf(raw[from.coerceAtMost(raw.lastIndex)])
            val avg = slice.average().toFloat()
            ((avg / max) * 100f).toInt().coerceIn(0, 100)
        }
    }
}
