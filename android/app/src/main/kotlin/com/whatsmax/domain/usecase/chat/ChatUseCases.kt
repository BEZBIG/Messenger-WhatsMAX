/**
 * domain/usecase/chat/ChatUseCases.kt
 * Use Cases для работы с чатами: создание (direct/group), получение
 * списка, управление участниками.
 */
package com.whatsmax.domain.usecase.chat

import com.whatsmax.domain.model.Chat
import com.whatsmax.domain.model.Result
import com.whatsmax.domain.repository.ChatRepository
import javax.inject.Inject

class GetChatsUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(): Result<List<Chat>> = repo.getChats()
}

class GetChatByIdUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<Chat> = repo.getChatById(chatId)
}

class CreateDirectChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(otherUserId: String): Result<Chat> =
        repo.createDirectChat(otherUserId)
}

class CreateGroupChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(name: String, memberUids: List<String>): Result<Chat> =
        repo.createGroupChat(name, memberUids)
}

class AddMemberToChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: String, userId: String): Result<Unit> =
        repo.addMember(chatId, userId)
}

class RemoveMemberFromChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: String, userId: String): Result<Unit> =
        repo.removeMember(chatId, userId)
}

class DeleteChatUseCase @Inject constructor(private val repo: ChatRepository) {
    suspend operator fun invoke(chatId: String): Result<Unit> = repo.deleteChat(chatId)
}
