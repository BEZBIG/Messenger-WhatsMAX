/** ViewModel главного экрана со списком чатов. */
package com.whatsmax.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.data.remote.dto.MessageDto
import com.whatsmax.domain.model.Chat
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.WebSocketRepository
import com.whatsmax.domain.usecase.auth.GetCurrentUserUseCase
import com.whatsmax.domain.usecase.auth.SignOutUseCase
import com.whatsmax.domain.usecase.chat.CreateDirectChatUseCase
import com.whatsmax.domain.usecase.chat.CreateGroupChatUseCase
import com.whatsmax.domain.usecase.chat.DeleteChatUseCase
import com.whatsmax.domain.usecase.chat.GetChatsUseCase
import com.whatsmax.domain.usecase.chat.RemoveMemberFromChatUseCase
import com.whatsmax.domain.usecase.user.SearchUsersUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

/** Состояние UI главного экрана. */
data class HomeUiState(
    val chats: List<Chat> = emptyList(),
    val currentUser: User? = null,
    val currentUserId: String = "",
    val searchResults: List<User> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

/** Загружает чаты, слушает WebSocket, управляет поиском и созданием чатов. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getChatsUseCase: GetChatsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchUsersUseCase: SearchUsersUseCase,
    private val createDirectChatUseCase: CreateDirectChatUseCase,
    private val createGroupChatUseCase: CreateGroupChatUseCase,
    private val deleteChatUseCase: DeleteChatUseCase,
    private val removeMemberUseCase: RemoveMemberFromChatUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val wsRepository: WebSocketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    private val jsonParser = Json { ignoreUnknownKeys = true }

    init {
        loadData()
        observeWebSocketMessages()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val user = getCurrentUserUseCase()
            _uiState.update { it.copy(currentUser = user, currentUserId = user?.uid ?: "") }

            when (val result = getChatsUseCase()) {
                is Result.Success -> _uiState.update { it.copy(chats = result.data, isLoading = false) }
                is Result.Error   -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                else -> Unit
            }
        }
    }

    /** Обновляет lastMessage и unreadCount при новом WS-сообщении. */
    private fun observeWebSocketMessages() {
        viewModelScope.launch {
            wsRepository.observeEvents().collect { event ->
                if (event.type == "NEW_MESSAGE") {
                    try {
                        val msgDto = jsonParser.decodeFromString<MessageDto>(event.payload)
                        val msg = msgDto.toModel()
                        val currentUserId = _uiState.value.currentUserId
                        _uiState.update { state ->
                            val updatedChats = state.chats.map { chat ->
                                if (chat.id == msg.chatId) {
                                    val isUnread = msg.senderId != currentUserId
                                    chat.copy(
                                        lastMessage = msg,
                                        unreadCount = if (isUnread) chat.unreadCount + 1 else chat.unreadCount
                                    )
                                } else chat
                            }
                            val sorted = updatedChats.sortedByDescending {
                                it.lastMessage?.createdAt ?: it.createdAt
                            }
                            state.copy(chats = sorted)
                        }
                    } catch (e: Exception) {
                        loadData()
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            viewModelScope.launch {
                when (val result = searchUsersUseCase(query)) {
                    is Result.Success -> _uiState.update { it.copy(searchResults = result.data) }
                    else -> Unit
                }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun openDirectChat(userId: String, onSuccess: (chatId: String) -> Unit) {
        viewModelScope.launch {
            when (val result = createDirectChatUseCase(userId)) {
                is Result.Success -> {
                    loadData()
                    onSuccess(result.data.id)
                }
                is Result.Error   -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    fun createGroupChat(name: String, memberIds: List<String>, onSuccess: (chatId: String) -> Unit) {
        viewModelScope.launch {
            when (val result = createGroupChatUseCase(name, memberIds)) {
                is Result.Success -> {
                    loadData()
                    onSuccess(result.data.id)
                }
                is Result.Error   -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    /** Удалить чат у текущего пользователя (soft-delete). */
    fun deleteChatForMe(chatId: String) {
        viewModelScope.launch {
            val userId = getCurrentUserUseCase()?.uid ?: return@launch
            when (val result = removeMemberUseCase(chatId, userId)) {
                is Result.Success -> _uiState.update { it.copy(chats = it.chats.filter { c -> c.id != chatId }) }
                is Result.Error   -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    /** Удалить чат у всех (физическое удаление, только создатель). */
    fun deleteChatForAll(chatId: String) {
        viewModelScope.launch {
            when (val result = deleteChatUseCase(chatId)) {
                is Result.Success -> _uiState.update { it.copy(chats = it.chats.filter { c -> c.id != chatId }) }
                is Result.Error   -> _uiState.update { it.copy(error = result.message) }
                else -> Unit
            }
        }
    }

    /** Оптимистичный сброс unreadCount при открытии чата. */
    fun markChatAsRead(chatId: String) {
        _uiState.update { state ->
            state.copy(chats = state.chats.map { chat ->
                if (chat.id == chatId) chat.copy(unreadCount = 0) else chat
            })
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
