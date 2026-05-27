/** Контракты репозиториев доменного слоя. */
package com.whatsmax.domain.repositories

import com.whatsmax.domain.models.*
import java.util.UUID

interface UserRepository {
    suspend fun createUser(uid: String, request: CreateUserRequest): User
    suspend fun getUserByUid(uid: String): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun searchUsers(query: String, limit: Int = 20): List<User>
    suspend fun updateUser(uid: String, request: UpdateUserRequest): User
    suspend fun updateOnlineStatus(uid: String, isOnline: Boolean)
    suspend fun updateFcmToken(uid: String, token: String)
    suspend fun deleteUser(uid: String)
}

interface ChatRepository {
    suspend fun createChat(request: CreateChatRequest, creatorUid: String): Chat
    suspend fun getChatById(chatId: UUID): Chat?
    suspend fun getUserChats(uid: String): List<Chat>
    suspend fun addMember(chatId: UUID, userId: String, role: String = "member")
    suspend fun removeMember(chatId: UUID, userId: String)
    suspend fun getChatMembers(chatId: UUID): List<ChatMember>
    suspend fun isUserInChat(chatId: UUID, userId: String): Boolean
    suspend fun getMemberRole(chatId: UUID, userId: String): String?
    suspend fun findDirectChat(uid1: String, uid2: String): Chat?
    suspend fun updateChat(chatId: UUID, name: String?, description: String?, avatarUrl: String?): Chat
    suspend fun deleteChat(chatId: UUID)
}

interface MessageRepository {
    suspend fun sendMessage(chatId: UUID, senderId: String, request: SendMessageRequest): Message
    suspend fun getChatMessages(chatId: UUID, limit: Int = 50, before: UUID? = null): List<Message>
    suspend fun getMessageById(messageId: UUID): Message?
    suspend fun getChatIdOf(messageId: UUID): UUID?
    suspend fun editMessage(messageId: UUID, content: String): Message
    suspend fun deleteMessage(messageId: UUID)
    suspend fun markAsRead(messageId: UUID, userId: String)
    suspend fun getUnreadCount(chatId: UUID, userId: String): Int
}

interface ChannelRepository {
    suspend fun createChannel(request: CreateChannelRequest, ownerId: String): Channel
    suspend fun getChannelById(channelId: UUID, requesterId: String? = null): Channel?
    suspend fun getChannelByHandle(handle: String): Channel?
    suspend fun searchChannels(query: String, limit: Int = 20): List<Channel>
    suspend fun getUserChannels(uid: String): List<Channel>
    suspend fun subscribeToChannel(channelId: UUID, userId: String)
    suspend fun unsubscribeFromChannel(channelId: UUID, userId: String)
    suspend fun getChannelSubscribers(channelId: UUID): List<User>
    suspend fun postMessage(channelId: UUID, authorId: String, request: SendMessageRequest): ChannelMessage
    suspend fun getChannelMessages(channelId: UUID, limit: Int = 50, before: UUID? = null): List<ChannelMessage>
    suspend fun registerMessageView(messageId: UUID, userId: String)
    suspend fun deleteChannel(channelId: UUID): List<UUID>
    suspend fun getComments(messageId: UUID): List<ChannelComment>
    suspend fun addComment(messageId: UUID, authorId: String, content: String): ChannelComment
    suspend fun getChannelIdOfPost(postId: UUID): UUID?
    suspend fun getChannelIdOfComment(commentId: UUID): UUID?
    suspend fun canViewChannel(channelId: UUID, userId: String): Boolean
}

interface ReactionRepository {
    suspend fun setReaction(entityType: String, entityId: UUID, userId: String, emoji: String): ReactionSummary
    suspend fun removeReaction(entityType: String, entityId: UUID, userId: String): ReactionSummary
    suspend fun getReactions(entityType: String, entityId: UUID, userId: String?): ReactionSummary
}

/** Метаданные файла в MinIO. */
data class StoredFileMeta(
    val id: UUID,
    val originalName: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String?,
    val objectKey: String?,
    val thumbObjectKey: String?,
    val storedName: String?
)

interface FileRepository {
    suspend fun saveFileInfo(originalName: String, storedName: String, mimeType: String, sizeBytes: Long, uploadedBy: String): FileInfo
    suspend fun saveObjectInfo(
        originalName: String, mimeType: String, sizeBytes: Long,
        uploadedBy: String, sha256: String, objectKey: String, thumbObjectKey: String?
    ): FileInfo
    suspend fun findBySha256(sha256: String): FileInfo?
    suspend fun getFileById(fileId: UUID): FileInfo?
    suspend fun getStoredName(fileId: UUID): String?
    suspend fun getMeta(fileId: UUID): StoredFileMeta?
    suspend fun deleteFile(fileId: UUID)
    suspend fun isOrphan(fileId: UUID): Boolean
}
