/**
 * presentation/channel/ChannelViewModel.kt
 * ViewModel экранов каналов: список/поиск, лента постов, комментарии,
 * подписка и реакции.
 */
package com.whatsmax.presentation.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.whatsmax.domain.model.Channel
import com.whatsmax.domain.model.ChannelComment
import com.whatsmax.domain.model.ChannelMessage
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.model.User
import com.whatsmax.domain.repository.FileRepository
import com.whatsmax.domain.usecase.auth.GetCurrentUserIdUseCase
import com.whatsmax.domain.usecase.channel.*
import com.whatsmax.domain.usecase.reaction.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ChannelListUiState(
    val myChannels: List<Channel> = emptyList(),
    val searchResults: List<Channel> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null
)

data class ChannelDetailUiState(
    val channel: Channel? = null,
    val messages: List<ChannelMessage> = emptyList(),
    val currentUserId: String = "",
    val postText: String = "",
    val isLoading: Boolean = false,
    val isPosting: Boolean = false,
    val isDeleting: Boolean = false,
    // Подписчики
    val subscribers: List<User> = emptyList(),
    val isLoadingSubscribers: Boolean = false,
    val showSubscribersSheet: Boolean = false,
    // Комментарии
    val expandedComments: Map<String, List<ChannelComment>> = emptyMap(),
    val commentTexts: Map<String, String> = emptyMap(),
    val loadingComments: Set<String> = emptySet(),
    val sendingComment: Set<String> = emptySet(),
    // Редактирование канала (владелец)
    val showEditSheet: Boolean = false,
    val editName: String = "",
    val editDescription: String = "",
    val isUpdating: Boolean = false,
    // Информация о канале (подписчик)
    val showInfoSheet: Boolean = false,
    // Реакции на посты: messageId -> emoji выбранный текущим юзером (пусто = нет реакции)
    val messageReactions: Map<String, String> = emptyMap(),
    // Реакции на комментарии: "messageId:commentId" -> emoji
    val commentReactions: Map<String, String> = emptyMap()
)

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val getMyChannelsUseCase: GetMyChannelsUseCase,
    private val searchChannelsUseCase: SearchChannelsUseCase,
    private val createChannelUseCase: CreateChannelUseCase,
    private val subscribeUseCase: SubscribeToChannelUseCase,
    private val unsubscribeUseCase: UnsubscribeFromChannelUseCase,
    private val getMessagesUseCase: GetChannelMessagesUseCase,
    private val postUseCase: PostToChannelUseCase,
    private val getChannelByIdUseCase: GetChannelByIdUseCase,
    private val updateChannelUseCase: UpdateChannelUseCase,
    private val deleteChannelUseCase: DeleteChannelUseCase,
    private val getSubscribersUseCase: GetChannelSubscribersUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val getReactionsUseCase: GetReactionsUseCase,
    private val setReactionUseCase: SetReactionUseCase,
    private val removeReactionUseCase: RemoveReactionUseCase,
    private val fileRepository: FileRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(ChannelListUiState())
    val listState: StateFlow<ChannelListUiState> = _listState

    private val _detailState = MutableStateFlow(ChannelDetailUiState())
    val detailState: StateFlow<ChannelDetailUiState> = _detailState

    init { loadMyChannels() }

    fun loadMyChannels() {
        viewModelScope.launch {
            _listState.update { it.copy(isLoading = true) }
            when (val r = getMyChannelsUseCase()) {
                is Result.Success -> _listState.update { it.copy(myChannels = r.data, isLoading = false) }
                is Result.Error   -> _listState.update { it.copy(error = r.message, isLoading = false) }
                else -> Unit
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _listState.update { it.copy(searchQuery = query) }
        if (query.length >= 2) {
            viewModelScope.launch {
                when (val r = searchChannelsUseCase(query)) {
                    is Result.Success -> _listState.update { it.copy(searchResults = r.data) }
                    else -> Unit
                }
            }
        } else {
            _listState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun loadChannelDetail(channelId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true, currentUserId = getCurrentUserIdUseCase() ?: "") }
            val channelResult  = getChannelByIdUseCase(channelId)
            val messagesResult = getMessagesUseCase(channelId)
            val messages = if (messagesResult is Result.Success) messagesResult.data else emptyList()
            _detailState.update { state ->
                val ch = if (channelResult is Result.Success) channelResult.data else state.channel
                state.copy(
                    channel      = ch,
                    messages     = messages,
                    isLoading    = false,
                    // Инициализируем поля редактирования текущими значениями канала
                    editName        = ch?.name ?: state.editName,
                    editDescription = ch?.description ?: state.editDescription
                )
            }
            if (messages.isNotEmpty()) loadPostReactions(messages)
        }
    }

    private fun loadPostReactions(messages: List<com.whatsmax.domain.model.ChannelMessage>) {
        viewModelScope.launch {
            messages.forEach { msg ->
                val r = getReactionsUseCase("channel_post", msg.id)
                if (r is Result.Success && r.data.myReaction != null) {
                    _detailState.update { state ->
                        state.copy(messageReactions = state.messageReactions + (msg.id to r.data.myReaction))
                    }
                }
            }
        }
    }

    fun subscribe(channelId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            when (subscribeUseCase(channelId)) {
                is Result.Success -> {
                    _detailState.update { s ->
                        s.copy(
                            isLoading = false,
                            channel = s.channel?.copy(
                                isSubscribed = true,
                                membersCount = s.channel.membersCount + 1
                            )
                        )
                    }
                    loadMyChannels()
                }
                else -> _detailState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun unsubscribe(channelId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoading = true) }
            when (unsubscribeUseCase(channelId)) {
                is Result.Success -> {
                    _detailState.update { s ->
                        s.copy(
                            isLoading = false,
                            channel = s.channel?.copy(
                                isSubscribed = false,
                                membersCount = maxOf(0, s.channel.membersCount - 1)
                            )
                        )
                    }
                    loadMyChannels()
                }
                else -> _detailState.update { it.copy(isLoading = false) }
            }
        }
    }

    // ─── Настройки канала (владелец) ────────────────────────────────────────

    fun openEditSheet() {
        val ch = _detailState.value.channel ?: return
        _detailState.update { it.copy(
            showEditSheet   = true,
            editName        = ch.name,
            editDescription = ch.description ?: ""
        ) }
    }

    fun closeEditSheet() = _detailState.update { it.copy(showEditSheet = false) }

    fun onEditNameChange(v: String) = _detailState.update { it.copy(editName = v) }
    fun onEditDescriptionChange(v: String) = _detailState.update { it.copy(editDescription = v) }

    fun saveChannelEdit(channelId: String) {
        val state = _detailState.value
        val name  = state.editName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            _detailState.update { it.copy(isUpdating = true) }
            when (val r = updateChannelUseCase(
                channelId   = channelId,
                name        = name,
                description = state.editDescription.trim().ifBlank { null }
            )) {
                is Result.Success -> _detailState.update { it.copy(
                    channel     = r.data,
                    isUpdating  = false,
                    showEditSheet = false
                ) }
                else -> _detailState.update { it.copy(isUpdating = false) }
            }
        }
    }

    // ─── Информация о канале (подписчик) ────────────────────────────────────

    fun openInfoSheet()  = _detailState.update { it.copy(showInfoSheet = true) }
    fun closeInfoSheet() = _detailState.update { it.copy(showInfoSheet = false) }

    // ─── Удаление ────────────────────────────────────────────────────────────

    fun deleteChannel(channelId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _detailState.update { it.copy(isDeleting = true) }
            when (deleteChannelUseCase(channelId)) {
                is Result.Success -> {
                    _listState.update { it.copy(myChannels = it.myChannels.filter { c -> c.id != channelId }) }
                    onDeleted()
                }
                is Result.Error   -> _detailState.update { it.copy(isDeleting = false) }
                else -> _detailState.update { it.copy(isDeleting = false) }
            }
        }
    }

    // ─── Подписчики ──────────────────────────────────────────────────────────

    fun loadSubscribers(channelId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(isLoadingSubscribers = true, showSubscribersSheet = true) }
            when (val r = getSubscribersUseCase(channelId)) {
                is Result.Success -> _detailState.update { it.copy(subscribers = r.data, isLoadingSubscribers = false) }
                else -> _detailState.update { it.copy(isLoadingSubscribers = false) }
            }
        }
    }

    fun hideSubscribersSheet() = _detailState.update { it.copy(showSubscribersSheet = false) }

    // ─── Создание канала ─────────────────────────────────────────────────────

    fun createChannel(handle: String, name: String, description: String, isPublic: Boolean) {
        viewModelScope.launch {
            _listState.update { it.copy(isCreating = true, error = null) }
            when (val r = createChannelUseCase(handle, name, description.ifBlank { null }, isPublic)) {
                is Result.Success -> _listState.update { it.copy(myChannels = it.myChannels + r.data, isCreating = false) }
                is Result.Error   -> _listState.update { it.copy(error = r.message, isCreating = false) }
                else -> Unit
            }
        }
    }

    fun clearError() = _listState.update { it.copy(error = null) }

    // ─── Посты ───────────────────────────────────────────────────────────────

    fun onPostTextChange(text: String) = _detailState.update { it.copy(postText = text) }

    fun postMessage(channelId: String) {
        val text = _detailState.value.postText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            _detailState.update { it.copy(isPosting = true, postText = "") }
            when (val r = postUseCase(channelId, text)) {
                is Result.Success -> _detailState.update { it.copy(messages = it.messages + r.data, isPosting = false) }
                is Result.Error   -> _detailState.update { it.copy(isPosting = false) }
                else -> Unit
            }
        }
    }

    /** Загрузить .m4a и опубликовать в канал как голосовое сообщение. */
    fun postVoice(channelId: String, file: File, durationMs: Long, waveform: List<Int>) {
        viewModelScope.launch {
            _detailState.update { it.copy(isPosting = true) }
            when (val r = fileRepository.uploadFile(file, "audio/mp4")) {
                is Result.Success -> {
                    when (val m = postUseCase(
                        channelId = channelId, content = null, type = "voice",
                        fileId = r.data.id, durationMs = durationMs, waveform = waveform
                    )) {
                        is Result.Success -> _detailState.update {
                            it.copy(messages = it.messages + m.data, isPosting = false)
                        }
                        else -> _detailState.update { it.copy(isPosting = false) }
                    }
                    runCatching { file.delete() }
                }
                else -> _detailState.update { it.copy(isPosting = false) }
            }
        }
    }

    // ─── Комментарии ────────────────────────────────────────────────────────

    fun toggleComments(channelId: String, messageId: String) {
        val state = _detailState.value
        if (state.expandedComments.containsKey(messageId)) {
            _detailState.update { it.copy(expandedComments = it.expandedComments - messageId) }
        } else {
            loadComments(channelId, messageId)
        }
    }

    private fun loadComments(channelId: String, messageId: String) {
        viewModelScope.launch {
            _detailState.update { it.copy(loadingComments = it.loadingComments + messageId) }
            when (val r = getCommentsUseCase(channelId, messageId)) {
                is Result.Success -> _detailState.update {
                    it.copy(
                        expandedComments = it.expandedComments + (messageId to r.data),
                        loadingComments  = it.loadingComments - messageId
                    )
                }
                else -> _detailState.update { it.copy(loadingComments = it.loadingComments - messageId) }
            }
        }
    }

    fun onCommentTextChange(messageId: String, text: String) {
        _detailState.update { it.copy(commentTexts = it.commentTexts + (messageId to text)) }
    }

    fun sendComment(channelId: String, messageId: String) {
        val text = _detailState.value.commentTexts[messageId]?.trim() ?: return
        if (text.isEmpty()) return
        viewModelScope.launch {
            _detailState.update {
                it.copy(
                    sendingComment = it.sendingComment + messageId,
                    commentTexts   = it.commentTexts + (messageId to "")
                )
            }
            when (val r = addCommentUseCase(channelId, messageId, text)) {
                is Result.Success -> _detailState.update { state ->
                    val updated = (state.expandedComments[messageId] ?: emptyList()) + r.data
                    val updatedMessages = state.messages.map { msg ->
                        if (msg.id == messageId) msg.copy(commentsCount = msg.commentsCount + 1) else msg
                    }
                    state.copy(
                        expandedComments = state.expandedComments + (messageId to updated),
                        sendingComment   = state.sendingComment - messageId,
                        messages         = updatedMessages
                    )
                }
                else -> _detailState.update { it.copy(sendingComment = it.sendingComment - messageId) }
            }
        }
    }

    // ─── Реакции на посты ────────────────────────────────────────────────────

    /** Ставим/снимаем реакцию на пост. Оптимистичное обновление + API. */
    fun toggleMessageReaction(messageId: String, emoji: String) {
        val current = _detailState.value.messageReactions[messageId]
        _detailState.update { state ->
            val updated = if (current == emoji) state.messageReactions - messageId
                          else state.messageReactions + (messageId to emoji)
            state.copy(messageReactions = updated)
        }
        viewModelScope.launch {
            val result = if (current == emoji)
                removeReactionUseCase("channel_post", messageId)
            else
                setReactionUseCase("channel_post", messageId, emoji)
            if (result is Result.Success) {
                _detailState.update { state ->
                    val serverEmoji = result.data.myReaction
                    val updated = if (serverEmoji != null) state.messageReactions + (messageId to serverEmoji)
                                  else state.messageReactions - messageId
                    state.copy(messageReactions = updated)
                }
            } else {
                _detailState.update { state ->
                    val reverted = if (current != null) state.messageReactions + (messageId to current)
                                   else state.messageReactions - messageId
                    state.copy(messageReactions = reverted)
                }
            }
        }
    }

    // ─── Реакции на комментарии ──────────────────────────────────────────────

    /** Ставим/снимаем реакцию на комментарий. Ключ = "messageId:commentId". Оптимистичное обновление + API. */
    fun toggleCommentReaction(messageId: String, commentId: String, emoji: String) {
        val key = "$messageId:$commentId"
        val current = _detailState.value.commentReactions[key]
        _detailState.update { state ->
            val updated = if (current == emoji) state.commentReactions - key
                          else state.commentReactions + (key to emoji)
            state.copy(commentReactions = updated)
        }
        viewModelScope.launch {
            val result = if (current == emoji)
                removeReactionUseCase("comment", commentId)
            else
                setReactionUseCase("comment", commentId, emoji)
            if (result is Result.Success) {
                _detailState.update { state ->
                    val serverEmoji = result.data.myReaction
                    val updated = if (serverEmoji != null) state.commentReactions + (key to serverEmoji)
                                  else state.commentReactions - key
                    state.copy(commentReactions = updated)
                }
            } else {
                _detailState.update { state ->
                    val reverted = if (current != null) state.commentReactions + (key to current)
                                   else state.commentReactions - key
                    state.copy(commentReactions = reverted)
                }
            }
        }
    }
}
