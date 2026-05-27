/**
 * presentation/call/CallScreen.kt
 * Экран аудио/видеозвонка. В CALLING/RINGING — full-screen своя камера
 * с оверлеем управления; в ONGOING — видео собеседника во весь экран
 * плюс перетаскиваемое PiP-окошко своей камеры.
 */
package com.whatsmax.presentation.call

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import com.whatsmax.presentation.theme.OnlineIndicator
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlin.math.roundToInt

@Composable
fun CallScreen(
    chatId: String,
    isVideo: Boolean,
    isIncoming: Boolean = false,
    onEnd: () -> Unit,
    viewModel: CallViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Разрешение камеры
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(chatId, isVideo) {
        viewModel.startCall(chatId, isVideo, isIncoming)
        if (isVideo && !hasCameraPermission) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state.callStatus) {
        if (state.callStatus == CallStatus.ENDED) onEnd()
    }

    val bgColor = if (isVideo) Color(0xFF0D1B2A) else Color(0xFF1A1A2E)
    val isOngoing = state.callStatus == CallStatus.ONGOING

    // Позиция PiP-окошка (своя камера, только при ONGOING)
    var pipOffset by remember { mutableStateOf(Offset(16f, 120f)) }

    Box(modifier = Modifier.fillMaxSize().background(bgColor)) {

        // ══════════════════════════════════════════════════════════════
        // ФОНОВЫЙ СЛОЙ — зависит от статуса звонка
        // ══════════════════════════════════════════════════════════════
        if (isVideo) {
            if (!isOngoing) {
                // CALLING / RINGING → своя камера во весь экран
                if (!state.isCameraOff && hasCameraPermission) {
                    LocalCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        useFrontCamera = state.isFrontCamera
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xFF0D1B2A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.VideocamOff, null,
                            tint = Color.White.copy(0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
                // Полупрозрачный оверлей поверх камеры для читаемости текста
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                )
            } else {
                // ONGOING → видео собеседника во весь экран (заглушка)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D1B2A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(0.4f),
                                shape = CircleShape,
                                modifier = Modifier.fillMaxSize()
                            ) {}
                            Text(
                                state.peerName.firstOrNull()?.uppercase() ?: "?",
                                fontSize = 42.sp, fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Аудиозвонок — тёмный фон
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
            )
        }

        // ── Тип звонка — вверху ─────────────────────────────────────
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.Phone,
                contentDescription = null,
                tint = Color.White.copy(0.7f),
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = if (isVideo) "Видеозвонок" else "Аудиозвонок",
                color = Color.White.copy(0.7f),
                fontSize = 14.sp
            )
        }

        // ── Центральная часть: аватар + имя + статус ────────────────
        // При видео ONGOING — имя/таймер показываем в углу, не по центру
        if (!isVideo || !isOngoing) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Аватар показываем только при аудиозвонке (при видео — камера на фоне)
                if (!isVideo) {
                    Box(
                        modifier = Modifier.size(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                        Text(
                            text = state.peerName.firstOrNull()?.uppercase() ?: "?",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
                Text(
                    state.peerName,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (state.callStatus) {
                        CallStatus.CALLING -> "Вызов..."
                        CallStatus.RINGING -> if (state.isIncoming)
                            "Входящий ${if (isVideo) "видеозвонок" else "звонок"}"
                        else "Ожидание ответа..."
                        CallStatus.ONGOING -> state.duration
                        CallStatus.ENDED   -> "Звонок завершён"
                    },
                    color = Color.White.copy(0.7f),
                    fontSize = 16.sp
                )
            }
        } else {
            // Имя собеседника + время в верхнем левом углу при ONGOING видеозвонке
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 80.dp)
            ) {
                Text(state.peerName, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text(state.duration, fontSize = 14.sp, color = Color.White.copy(0.7f))
            }
        }

        // ══════════════════════════════════════════════════════════════
        // PiP-ОКОШКО — своя камера (перетаскивается)
        // Показывается только при видеозвонке в режиме ONGOING
        // ══════════════════════════════════════════════════════════════
        if (isVideo && isOngoing) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(pipOffset.x.roundToInt(), pipOffset.y.roundToInt()) }
                    .size(width = 108.dp, height = 144.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1C2E40))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            pipOffset = Offset(
                                x = (pipOffset.x + dragAmount.x).coerceAtLeast(0f),
                                y = (pipOffset.y + dragAmount.y).coerceAtLeast(0f)
                            )
                        }
                    }
            ) {
                if (!state.isCameraOff && hasCameraPermission) {
                    LocalCameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        useFrontCamera = state.isFrontCamera
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.VideocamOff, null,
                            tint = Color.White.copy(0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    "Вы",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(4.dp),
                    fontSize = 10.sp,
                    color = Color.White.copy(0.8f)
                )
            }
        }

        // ── Кнопки управления — внизу ──────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            if (state.isIncoming && state.callStatus == CallStatus.RINGING) {
                Row(horizontalArrangement = Arrangement.spacedBy(64.dp)) {
                    CallButton(
                        icon    = Icons.Default.CallEnd,
                        label   = "Отклонить",
                        color   = Color.Red,
                        onClick = { viewModel.declineCall() },
                        large   = true
                    )
                    CallButton(
                        icon    = if (isVideo) Icons.Default.Videocam else Icons.Default.Call,
                        label   = "Принять",
                        color   = OnlineIndicator,
                        onClick = viewModel::acceptCall,
                        large   = true
                    )
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Видеокнопки — над кнопкой завершения
                    if (isVideo) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)
                        ) {
                            CallButton(
                                icon    = if (state.isCameraOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                label   = if (state.isCameraOff) "Камера выкл." else "Камера вкл.",
                                color   = if (state.isCameraOff) Color.Gray else OnlineIndicator,
                                onClick = viewModel::toggleCamera
                            )
                            CallButton(
                                icon    = Icons.Default.Cameraswitch,
                                label   = "Перевернуть",
                                color   = Color.White.copy(0.2f),
                                onClick = viewModel::switchCamera
                            )
                        }
                    }
                    // Основной ряд: Микрофон | Завершить | Динамик/Пустышка
                    // Пустышка справа при видеозвонке балансирует Микрофон слева → End строго по центру
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CallButton(
                            icon    = if (state.isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label   = if (state.isMicMuted) "Вкл. мик." else "Выкл. мик.",
                            color   = if (state.isMicMuted) Color.Gray else Color.White.copy(0.2f),
                            onClick = viewModel::toggleMic
                        )
                        CallButton(
                            icon    = Icons.Default.CallEnd,
                            label   = "Завершить",
                            color   = Color.Red,
                            onClick = { viewModel.endCall() },
                            large   = true
                        )
                        if (!isVideo) {
                            CallButton(
                                icon    = if (state.isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                label   = "Динамик",
                                color   = if (state.isSpeakerOn) MaterialTheme.colorScheme.primary else Color.White.copy(0.2f),
                                onClick = viewModel::toggleSpeaker
                            )
                        } else {
                            // Невидимый балансир — End строго по центру
                            Box(Modifier.size(56.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── PiP камера с поддержкой переключения фронт/зад ──────────────────────────

@Composable
private fun LocalCameraPreview(modifier: Modifier = Modifier, useFrontCamera: Boolean = true) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // key() перерисовывает AndroidView при смене камеры
    key(useFrontCamera) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try {
                        cameraProvider.unbindAll()
                        val selector = when {
                            useFrontCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            else -> CameraSelector.DEFAULT_BACK_CAMERA
                        }
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                    } catch (e: Exception) { /* камера недоступна */ }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            },
            modifier = modifier
        )
    }
}

@Composable
private fun CallButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    large: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick  = onClick,
            modifier = Modifier.size(if (large) 72.dp else 56.dp)
        ) {
            Surface(color = color, shape = CircleShape, modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = Color.White, modifier = Modifier.size(if (large) 32.dp else 24.dp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White.copy(0.7f), fontSize = 11.sp)
    }
}
