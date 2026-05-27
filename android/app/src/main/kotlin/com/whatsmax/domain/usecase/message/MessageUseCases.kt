/**
 * domain/usecase/message/MessageUseCases.kt
 * Use Cases сообщений: отправка, получение, редактирование, удаление,
 * подписка на real-time события.
 */
package com.whatsmax.domain.usecase.message

import com.whatsmax.domain.model.Message
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(private val repo: MessageRepository) {
    suspend operator fun invoke(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>> =
        repo.getMessages(chatId, limit, before)
}

class SendMessageUseCase @Inject constructor(private val repo: MessageRepository) {
    suspend operator fun invoke(
        chatId: String, content: String?,
        type: String = "text", fileId: String? = null, replyToId: String? = null,
        durationMs: Long? = null, waveform: List<Int>? = null
    ): Result<Message> = repo.sendMessage(chatId, content, type, fileId, replyToId, durationMs, waveform)
}

class EditMessageUseCase @Inject constructor(private val repo: MessageRepository) {
    suspend operator fun invoke(chatId: String, messageId: String, content: String): Result<Message> =
        repo.editMessage(chatId, messageId, content)
}

class DeleteMessageUseCase @Inject constructor(private val repo: MessageRepository) {
    suspend operator fun invoke(chatId: String, messageId: String): Result<Unit> =
        repo.deleteMessage(chatId, messageId)
}

class ObserveMessagesUseCase @Inject constructor(private val repo: MessageRepository) {
    operator fun invoke(chatId: String): Flow<Message> = repo.observeMessages(chatId)
}

class MarkAsReadUseCase @Inject constructor(private val repo: MessageRepository) {
    suspend operator fun invoke(chatId: String, messageId: String): Result<Unit> =
        repo.markAsRead(chatId, messageId)
}
