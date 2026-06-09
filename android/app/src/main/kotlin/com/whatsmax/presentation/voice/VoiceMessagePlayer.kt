/** Composable-плеер голосовых: play/pause, waveform, MediaPlayer. */
package com.whatsmax.presentation.voice

import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.whatsmax.R
import kotlinx.coroutines.delay

@Composable
fun VoiceMessagePlayer(
    audioUrl: String,
    durationMs: Long?,
    waveform: List<Int>?,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    var isPlaying by remember(audioUrl) { mutableStateOf(false) }
    var positionMs by remember(audioUrl) { mutableStateOf(0L) }
    var totalMs by remember(audioUrl) { mutableStateOf(durationMs ?: 0L) }
    val player = remember(audioUrl) { MediaPlayer() }

    DisposableEffect(audioUrl) {
        runCatching {
            player.setDataSource(audioUrl)
            player.setOnPreparedListener {
                if (totalMs <= 0L) totalMs = it.duration.toLong()
            }
            player.setOnCompletionListener {
                isPlaying = false
                positionMs = totalMs
            }
            player.prepareAsync()
        }
        onDispose {
            runCatching { player.stop() }
            runCatching { player.release() }
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = runCatching { player.currentPosition.toLong() }.getOrDefault(positionMs)
            delay(50)
        }
    }

    val progress = if (totalMs > 0) (positionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(min = 200.dp, max = 240.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    runCatching { player.pause() }
                    isPlaying = false
                } else {
                    if (positionMs >= totalMs && totalMs > 0L) {
                        runCatching { player.seekTo(0) }
                        positionMs = 0L
                    }
                    runCatching { player.start() }
                    isPlaying = true
                }
            },
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(tint)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                tint = Color.White
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            WaveformBar(
                waveform = waveform,
                progress = progress,
                color    = tint
            )
            Text(
                text     = formatMmSs(if (isPlaying || positionMs in 1 until totalMs) positionMs else totalMs),
                fontSize = 11.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun WaveformBar(
    waveform: List<Int>?,
    progress: Float,
    color: Color
) {
    val points = waveform ?: List(VoiceRecorder.WAVEFORM_POINTS) { 15 }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val barCount = points.size
        if (barCount == 0) return@Canvas
        val gap = 1.5f
        val barWidth = (size.width - gap * (barCount - 1)) / barCount
        val centerY = size.height / 2f
        val playedBars = (barCount * progress).toInt()
        points.forEachIndexed { i, amp ->
            val h = (size.height * (amp.coerceAtLeast(10) / 100f)).coerceAtLeast(2f)
            val x = i * (barWidth + gap) + barWidth / 2f
            val barColor = if (i < playedBars) color else color.copy(alpha = 0.35f)
            drawLine(
                color = barColor,
                start = Offset(x, centerY - h / 2f),
                end   = Offset(x, centerY + h / 2f),
                strokeWidth = barWidth.coerceAtLeast(1.5f),
                cap   = StrokeCap.Round
            )
        }
    }
}

private fun formatMmSs(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
