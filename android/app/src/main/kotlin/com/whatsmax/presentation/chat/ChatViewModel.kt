/** ViewModel экрана чата. */
package com.whatsmax.presentation.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.domain.model.Chat
import com.whatsmax.domain.model.Message
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.FileRepository
import com.whatsmax.domain.repository.ReactionSummary
import com.whatsmax.domain.usecase.auth.GetCurrentUserUseCase
import com.whatsmax.domain.usecase.chat.GetChatByIdUseCase
import com.whatsmax.domain.usecase.message.*
import com.whatsmax.domain.usecase.reaction.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/** Состояние UI экрана чата. */
data class ChatUiState(
    val chat: Chat? = null,
    val messages: List<Message> = emptyList(),
    val currentUserId: String = "",
    val inputText: String = "",
    val replyToMessage: Message? = null,
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isTyping: Boolean = false,
    val error: String? = null,
    val messageReactions: Map<String, String> = emptyMap()
)

/** Управляет историей сообщений, real-time обновлениями, реакциями и отправкой. */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val editMessageUseCase: EditMessageUseCase,
    private val deleteMessageUseCase: DeleteMessageUseCase,
    private val observeMessagesUseCase: ObserveMessagesUseCase,
    private val markAsReadUseCase: MarkAsReadUseCase,
    private val getChatByIdUseCase: GetChatByIdUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val fileRepository: FileRepository,
    private val getReactionsUseCase: GetReactionsUseCase,
    private val setReactionUseCase: SetReactionUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    fun init(chatId: String) {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _uiState.update { it.copy(currentUserId = user?.uid ?: "") }

            when (val r = getChatByIdUseCase(chatId)) {
                is Result.Success -> _uiState.update { it.copy(chat = r.data) }
                else -> Unit
            }

            loadMessages(chatId)
            observeRealTimeMessages(chatId)
        }
    }

    private fun loadMessages(chatId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val r = getMessagesUseCase(chatId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(messages = r.data, isLoading = false) }
                    markLoadedMessagesAsRead(chatId, r.data)
                    loadReactionsForMessages(r.data)
                }
                is Result.Error -> _uiState.update { it.copy(error = r.message, isLoading = false) }
                else -> Unit
            }
        }
    }

    private fun markLoadedMessagesAsRead(chatId: String, messages: List<Message>) {
        val currentUserId = _uiState.value.currentUserId
        if (currentUserId.isEmpty()) return
        viewModelScope.launch {
            messages
                .filter { it.senderId != currentUserId && it.readBy.none { uid -> uid == currentUserId } }
                .forEach { markAsReadUseCase(chatId, it.id) }
        }
    }

    private fun observeRealTimeMessages(chatId: String) {
        viewModelScope.launch {
            observeMessagesUseCase(chatId).collect { newMsg ->
                _uiState.update { state ->
                    val existing = state.messages.indexOfFirst { it.id == newMsg.id }
                    val updated = if (existing >= 0) {
                        state.messages.toMutableList().also { it[existing] = newMsg }
                    } else {
                        state.messages + newMsg
                    }
                    state.copy(messages = updated)
                }
                markAsReadUseCase(chatId, newMsg.id)
            }
        }
    }

    fun onInputChange(text: String) = _uiState.update { it.copy(inputText = text) }

    fun setReplyTo(message: Message?) = _uiState.update { it.copy(replyToMessage = message) }

    fun sendMessage(chatId: String) {
        val state = _uiState.value
        val content = state.inputText.trim()
        if (content.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "", replyToMessage = null) }
            when (val r = sendMessageUseCase(
                chatId    = chatId,
                content   = content,
                replyToId = state.replyToMessage?.id
            )) {
                is Result.Success -> _uiState.update { it.copy(
                    messages  = it.messages + r.data,
                    isSending = false
                ) }
                is Result.Error   -> _uiState.update { it.copy(isSending = false, error = r.message) }
                else              -> _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun sendFile(chatId: String, fileId: String, type: String) {
        viewModelScope.launch {
            sendMessageUseCase(chatId = chatId, content = null, type = type, fileId = fileId)
        }
    }

    fun uploadAndSendImage(chatId: String, file: File) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            when (val r = fileRepository.uploadFile(file, "image/jpeg")) {
                is Result.Success -> {
                    when (val m = sendMessageUseCase(
                        chatId = chatId, content = null, type = "image", fileId = r.data.id
                    )) {
                        is Result.Success -> _uiState.update { it.copy(
                            messages = it.messages + m.data, isSending = false
                        ) }
                        is Result.Error -> _uiState.update { it.copy(isSending = false, error = m.message) }
                        else -> _uiState.update { it.copy(isSending = false) }
                    }
                }
                is Result.Error -> _uiState.update { it.copy(isSending = false, error = r.message) }
                else -> _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    /** Загружает .m4a и отправляет как голосовое сообщение. */
    fun uploadAndSendVoice(chatId: String, file: File, durationMs: Long, waveform: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true) }
            when (val r = fileRepository.uploadFile(file, "audio/mp4")) {
                is Result.Success -> {
                    when (val m = sendMessageUseCase(
                        chatId     = chatId,
                        content    = null,
                        type       = "voice",
                        fileId     = r.data.id,
                        durationMs = durationMs,
                        waveform   = waveform
                    )) {
                        is Result.Success -> _uiState.update { it.copy(
                            messages = it.messages + m.data, isSending = false
                        ) }
                        is Result.Error -> _uiState.update { it.copy(isSending = false, error = m.message) }
                        else -> _uiState.update { it.copy(isSending = false) }
                    }
                    runCatching { file.delete() }
                }
                is Result.Error -> _uiState.update { it.copy(isSending = false, error = r.message) }
                else -> _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun editMessage(chatId: String, messageId: String, content: String) {
        viewModelScope.launch {
            editMessageUseCase(chatId, messageId, content)
        }
    }

    fun deleteMessage(chatId: String, messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(chatId, messageId)
            _uiState.update { state ->
                state.copy(messages = state.messages.map {
                    if (it.id == messageId) it.copy(isDeleted = true, content = "Сообщение удалено")
                    else it
                })
            }
        }
    }

    /** Ставит/снимает реакцию с оптимистичным обновлением. */
    fun toggleReaction(messageId: String, emoji: String) {
        val current = _uiState.value.messageReactions[messageId]
        _uiState.update { state ->
            val updated = if (current == emoji)
                state.messageReactions - messageId
            else
                state.messageReactions + (messageId to emoji)
            state.copy(messageReactions = updated)
        }
        viewModelScope.launch {
            val result = if (current == emoji)
                removeReactionUseCase("message", messageId)
            else
                setReactionUseCase("message", messageId, emoji)
            if (result is Result.Success) {
                _uiState.update { state ->
                    val serverEmoji = result.data.myReaction
                    val updated = if (serverEmoji != null)
                        state.messageReactions + (messageId to serverEmoji)
                    else
                        state.messageReactions - messageId
                    state.copy(messageReactions = updated)
                }
            } else {
                _uiState.update { state ->
                    val reverted = if (current != null)
                        state.messageReactions + (messageId to current)
                    else
                        state.messageReactions - messageId
                    state.copy(messageReactions = reverted)
                }
            }
        }
    }

    private fun loadReactionsForMessages(messages: List<Message>) {
        viewModelScope.launch {
            messages.forEach { msg ->
                val r = getReactionsUseCase("message", msg.id)
                if (r is Result.Success && r.data.myReaction != null) {
                    _uiState.update { state ->
                        state.copy(messageReactions = state.messageReactions + (msg.id to r.data.myReaction))
                    }
                }
            }
        }
    }

    /** Преобразует fileId в абсолютный URL для MediaPlayer/Coil. */
    suspend fun resolveFileUrl(fileId: String): String = fileRepository.getFileUrl(fileId)

    fun loadMoreMessages(chatId: String) {
        val oldestId = _uiState.value.messages.firstOrNull()?.id ?: return
        viewModelScope.launch {
            when (val r = getMessagesUseCase(chatId, 50, oldestId)) {
                is Result.Success -> _uiState.update { it.copy(messages = r.data + it.messages) }
                else -> Unit
            }
        }
    }
}
