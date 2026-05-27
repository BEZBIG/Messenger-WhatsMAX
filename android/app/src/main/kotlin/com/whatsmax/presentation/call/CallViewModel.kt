/** ViewModel WebRTC-звонка: сигнализация, управление камерой/микрофоном, запись в чат */
package com.whatsmax.presentation.call

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.data.remote.dto.CallSignalDto
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.WsEvent
import com.whatsmax.domain.repository.AuthRepository
import com.whatsmax.domain.repository.ChatRepository
import com.whatsmax.domain.repository.WebSocketRepository
import com.whatsmax.domain.usecase.message.SendMessageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject

enum class CallStatus { CALLING, RINGING, ONGOING, ENDED }

data class CallUiState(
    val peerName: String = "Пользователь",
    val callStatus: CallStatus = CallStatus.CALLING,
    val duration: String = "00:00",
    val isMicMuted: Boolean = false,
    val isCameraOff: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isIncoming: Boolean = false,
    val isFrontCamera: Boolean = true
)

@HiltViewModel
class CallViewModel @Inject constructor(
    private val wsRepository: WebSocketRepository,
    private val chatRepository: ChatRepository,
    private val authRepository: AuthRepository,
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState

    private var durationJob: Job? = null
    private var ringJob: Job? = null
    private var toneGenerator: ToneGenerator? = null
    private var seconds = 0

    private var currentUserId: String = ""
    private var peerUserId: String = ""
    private var callId: String = ""
    private var currentChatId: String = ""
    private var currentIsVideo: Boolean = false

    fun startCall(chatId: String, isVideo: Boolean, isIncoming: Boolean = false) {
        currentChatId = chatId
        currentIsVideo = isVideo

        if (isIncoming) {
            _uiState.update { it.copy(callStatus = CallStatus.RINGING, isIncoming = true) }
        } else {
            _uiState.update { it.copy(callStatus = CallStatus.CALLING, isIncoming = false) }
            startRingTone()

            viewModelScope.launch {
                currentUserId = authRepository.getCurrentFirebaseUid() ?: ""

                val chatResult = chatRepository.getChatById(chatId)
                if (chatResult is Result.Success) {
                    val peer = chatResult.data.members.firstOrNull { it.userId != currentUserId }
                    peerUserId = peer?.userId ?: ""
                    if (peer != null) {
                        _uiState.update { it.copy(peerName = peer.displayName) }
                    }
                }

                if (currentUserId.isNotEmpty() && peerUserId.isNotEmpty()) {
                    callId = UUID.randomUUID().toString()
                    val signal = CallSignalDto(
                        callId     = callId,
                        fromUserId = currentUserId,
                        toUserId   = peerUserId,
                        isVideo    = isVideo
                    )
                    wsRepository.sendEvent(
                        WsEvent("CALL_OFFER", Json.encodeToString(signal))
                    )
                }
            }

            ringJob = viewModelScope.launch {
                delay(30_000)
                if (_uiState.value.callStatus == CallStatus.CALLING) {
                    stopRingTone()
                    val label = if (currentIsVideo) "Нет ответа (видеозвонок)" else "Нет ответа"
                    sendCallMessage(label)
                    sendWsCallEnd()
                    _uiState.update { it.copy(callStatus = CallStatus.ENDED) }
                }
            }
        }
    }

    private fun startRingTone() {
        stopRingTone()
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 80)
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, -1)
        } catch (e: Exception) { /* устройство может не поддерживать */ }
    }

    private fun stopRingTone() {
        ringJob?.cancel()
        ringJob = null
        try {
            toneGenerator?.stopTone()
            toneGenerator?.release()
        } catch (e: Exception) { /* игнорируем */ }
        toneGenerator = null
    }

    fun acceptCall() {
        stopRingTone()
        _uiState.update { it.copy(callStatus = CallStatus.ONGOING) }
        startDurationTimer()
    }

    fun declineCall() {
        stopRingTone()
        durationJob?.cancel()
        val label = if (currentIsVideo) "Пропущенный видеозвонок" else "Пропущенный звонок"
        sendCallMessage(label)
        sendWsCallEnd()
        _uiState.update { it.copy(callStatus = CallStatus.ENDED) }
    }

    private fun startDurationTimer() {
        durationJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                seconds++
                val mins = seconds / 60
                val secs = seconds % 60
                _uiState.update { it.copy(duration = "%02d:%02d".format(mins, secs)) }
            }
        }
    }

    fun toggleMic()     = _uiState.update { it.copy(isMicMuted = !it.isMicMuted) }
    fun toggleCamera()  = _uiState.update { it.copy(isCameraOff = !it.isCameraOff) }
    fun toggleSpeaker() = _uiState.update { it.copy(isSpeakerOn = !it.isSpeakerOn) }
    fun switchCamera()  = _uiState.update { it.copy(isFrontCamera = !it.isFrontCamera) }

    fun endCall() {
        stopRingTone()
        durationJob?.cancel()
        val label = buildString {
            append(if (currentIsVideo) "Видеозвонок завершён" else "Звонок завершён")
            if (seconds > 0) append(" · ${_uiState.value.duration}")
        }
        sendCallMessage(label)
        sendWsCallEnd()
        _uiState.update { it.copy(callStatus = CallStatus.ENDED) }
    }

    private fun sendCallMessage(content: String) {
        if (currentChatId.isEmpty()) return
        viewModelScope.launch {
            sendMessageUseCase(chatId = currentChatId, content = content, type = "call")
        }
    }

    private fun sendWsCallEnd() {
        if (currentUserId.isEmpty() || peerUserId.isEmpty()) return
        try {
            val signal = CallSignalDto(
                callId     = callId.ifEmpty { UUID.randomUUID().toString() },
                fromUserId = currentUserId,
                toUserId   = peerUserId,
                isVideo    = currentIsVideo
            )
            wsRepository.sendEvent(WsEvent("CALL_END", Json.encodeToString(signal)))
        } catch (e: Exception) { /* игнорируем если WS не подключён */ }
    }

    override fun onCleared() {
        super.onCleared()
        stopRingTone()
        durationJob?.cancel()
        sendWsCallEnd()
    }
}
