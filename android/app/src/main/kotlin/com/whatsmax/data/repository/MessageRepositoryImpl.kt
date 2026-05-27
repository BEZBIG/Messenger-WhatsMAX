/** Реализация MessageRepository: REST + WebSocket real-time. */
package com.whatsmax.data.repository

import com.whatsmax.data.remote.api.ApiService
import com.whatsmax.data.remote.dto.EditMessageRequest
import com.whatsmax.data.remote.dto.SendMessageRequest
import com.whatsmax.data.remote.dto.WsEventDto
import com.whatsmax.data.remote.websocket.WebSocketClient
import com.whatsmax.domain.model.Message
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val WS_EVENT_NEW_MESSAGE     = "new_message"
private const val WS_EVENT_MESSAGE_EDITED  = "message_edited"
private const val WS_EVENT_MESSAGE_DELETED = "message_deleted"

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val wsClient: WebSocketClient
) : MessageRepository {

    override suspend fun getMessages(chatId: String, limit: Int, before: String?): Result<List<Message>> =
        safeApiCall {
            apiService.getMessages(chatId, limit, before).bodyOrThrow().map { it.toModel() }
        }

    override suspend fun sendMessage(
        chatId: String, content: String?, type: String,
        fileId: String?, replyToId: String?,
        durationMs: Long?, waveform: List<Int>?
    ): Result<Message> = safeApiCall {
        apiService.sendMessage(chatId, SendMessageRequest(content, type, fileId, replyToId, durationMs, waveform))
            .bodyOrThrow().toModel()
    }

    override suspend fun editMessage(chatId: String, messageId: String, content: String): Result<Message> =
        safeApiCall {
            apiService.editMessage(chatId, messageId, EditMessageRequest(content))
                .bodyOrThrow().toModel()
        }

    override suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit> =
        safeApiCall { apiService.deleteMessage(chatId, messageId); Unit }

    override suspend fun markAsRead(chatId: String, messageId: String): Result<Unit> =
        safeApiCall { apiService.markAsRead(chatId, messageId); Unit }

    override fun observeMessages(chatId: String): Flow<Message> =
        wsClient.events.mapNotNull { event ->
            when (event.type) {
                WS_EVENT_NEW_MESSAGE, WS_EVENT_MESSAGE_EDITED -> {
                    try {
                        val msg = Json.decodeFromString<com.whatsmax.data.remote.dto.MessageDto>(event.payload).toModel()
                        if (msg.chatId == chatId) msg else null
                    } catch (e: Exception) { null }
                }
                else -> null
            }
        }
}
