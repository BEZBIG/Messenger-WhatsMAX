/** Интерфейсы репозиториев domain-слоя. */
package com.whatsmax.domain.repository

import com.whatsmax.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.io.File

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String, username: String, displayName: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signOut()
    suspend fun getCurrentUser(): User?
    fun getCurrentFirebaseUid(): String?
    fun getFirebaseIdToken(): Flow<String?>
    fun isUserLoggedIn(): Boolean
}

interface UserRepository {
    suspend fun getUserByUid(uid: String): Result<User>
    suspend fun searchUsers(query: String): Result<List<User>>
    suspend fun updateProfile(displayName: String?, bio: String?, username: String?, avatarUrl: String? = null): Result<User>
    suspend fun updateFcmToken(token: String): Result<Unit>
}

interface ChatRepository {
    suspend fun createDirectChat(otherUserId: String): Result<Chat>
    suspend fun createGroupChat(name: String, memberUids: List<String>): Result<Chat>
    suspend fun getChats(): Result<List<Chat>>
    suspend fun getChatById(chatId: String): Result<Chat>
    suspend fun addMember(chatId: String, userId: String): Result<Unit>
    suspend fun removeMember(chatId: String, userId: String): Result<Unit>
    suspend fun deleteChat(chatId: String): Result<Unit>
    suspend fun updateChat(chatId: String, name: String?, description: String?): Result<Chat>
}

interface MessageRepository {
    suspend fun sendMessage(
        chatId: String, content: String?, type: String = "text",
        fileId: String? = null, replyToId: String? = null,
        durationMs: Long? = null, waveform: List<Int>? = null
    ): Result<Message>
    suspend fun getMessages(chatId: String, limit: Int = 50, before: String? = null): Result<List<Message>>
    suspend fun editMessage(chatId: String, messageId: String, content: String): Result<Message>
    suspend fun deleteMessage(chatId: String, messageId: String): Result<Unit>
    suspend fun markAsRead(chatId: String, messageId: String): Result<Unit>
    fun observeMessages(chatId: String): Flow<Message>  // real-time via WS
}

interface ChannelRepository {
    suspend fun createChannel(handle: String, name: String, description: String?, isPublic: Boolean): Result<Channel>
    suspend fun searchChannels(query: String): Result<List<Channel>>
    suspend fun getMyChannels(): Result<List<Channel>>
    suspend fun getChannelById(channelId: String): Result<Channel>
    suspend fun subscribeToChannel(channelId: String): Result<Unit>
    suspend fun unsubscribeFromChannel(channelId: String): Result<Unit>
    suspend fun getChannelSubscribers(channelId: String): Result<List<com.whatsmax.domain.model.User>>
    suspend fun getChannelMessages(channelId: String, limit: Int = 50): Result<List<ChannelMessage>>
    suspend fun postToChannel(
        channelId: String, content: String?, type: String = "text",
        fileId: String? = null, durationMs: Long? = null, waveform: List<Int>? = null
    ): Result<ChannelMessage>
    suspend fun updateChannel(channelId: String, name: String?, description: String?): Result<Channel>
    suspend fun deleteChannel(channelId: String): Result<Unit>
    suspend fun getComments(channelId: String, messageId: String): Result<List<ChannelComment>>
    suspend fun addComment(channelId: String, messageId: String, content: String): Result<ChannelComment>
}

interface FileRepository {
    suspend fun uploadFile(file: File, mimeType: String): Result<FileInfo>
    suspend fun getFileUrl(fileId: String): String
}

data class ReactionSummary(
    val reactions: Map<String, Int> = emptyMap(),
    val myReaction: String? = null
)

interface ReactionRepository {
    suspend fun getReactions(entityType: String, entityId: String): Result<ReactionSummary>
    suspend fun setReaction(entityType: String, entityId: String, emoji: String): Result<ReactionSummary>
    suspend fun removeReaction(entityType: String, entityId: String): Result<ReactionSummary>
}

interface WebSocketRepository {
    fun connect(token: String)
    fun disconnect()
    fun sendEvent(event: WsEvent)
    fun observeEvents(): Flow<WsEvent>
    fun isConnected(): Boolean
}
